#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "common.h"
#include "vm.h"
#include "debug.h"

VM vm;
size_t ptrSize = sizeof(void*);

static void resetStack() {
	vm.stackTop = vm.stack;
}

void initVM(char* basePath) {
	vm.basePath = basePath;
	vm.chunks = NULL;
	vm.chunkCount = 0;
	vm.currentChunkIndex = 0;
	vm.ip = NULL;
	vm.stackTop = NULL;
	vm.input = NULL;
	vm.inputEnd = NULL;
	vm.inputCursor = NULL;
	resetStack();
	setVMDebugTrace(0);
}

void setVMDebugTrace(int debugTrace) {
	vm.debugTrace = debugTrace;
}

void freeVM(void) {
	for (int i = 0; i < vm.chunkCount; i++) {
		Chunk* chunk = getChunkByIndex(i);
		freeChunk(chunk);
		free(chunk);
	}
	free(vm.chunks);
	free(vm.input);
}

char scanChar(void) {
	if (vm.inputCursor >= vm.inputEnd) {
		size_t oldLen = vm.inputEnd - vm.inputCursor;
		size_t newLen = oldLen + 1;
		char* newInput = malloc(newLen);
		if (!newInput) return NULL;
		memcpy(newInput, vm.inputCursor, oldLen);
		scanf_s("%c", &newInput[newLen - 1], 1);
		
		free(vm.input);
		vm.input = newInput;
		vm.inputCursor = newInput;
		vm.inputEnd = newInput + newLen;
	}
	return *(vm.inputCursor++);
}

int addChunk(Chunk* chunk) {
	Chunk** chunks = realloc(vm.chunks, sizeof(Chunk*) * ((size_t)vm.chunkCount + 1));
	if (!chunks) return 0;
	vm.chunks = chunks;
	vm.chunks[vm.chunkCount] = chunk;
	vm.chunkCount++;
	return 1;
}

int readAndAddChunk(char* name) {
	Chunk* chunk = readChunk(vm.basePath, name);
	if (!chunk) return 0;
	return addChunk(chunk);
}

Chunk* getChunkByIndex(uint8_t index) {
	if (index >= vm.chunkCount) return NULL;
	return vm.chunks[index];
}

Chunk* getChunkByName(char* name) {
	for (int i = 0; i < vm.chunkCount; i++) {
		Chunk* chunk = getChunkByIndex(i);
		if (!chunk) return NULL;
		int ba = strlen(chunk->name);
		int b = ba != strlen(name);
		if (b) continue;
		if (!arraysEqual(chunk->name, name, strlen(name))) continue;
		return chunk;
	}
	return NULL;
}

int setCurrentChunkByName(char* name) {
	for (int i = 0; i < vm.chunkCount; i++) {
		Chunk* chunk = getChunkByIndex(i);
		if (strlen(chunk->name) != strlen(name)) continue;
		if (!arraysEqual(chunk->name, name, strlen(name))) continue;
		vm.currentChunkIndex = i;
		vm.ip = chunk->code;
		return 1;
	}
	return 0;
}

int setCurrentChunkByPtr(void* ptr) {
	for (int i = 0; i < vm.chunkCount; i++) {
		Chunk* chunk = getChunkByIndex(i);
		if (ptr < chunk->code) continue;
		if (ptr > (void*)(chunk->code + chunk->count)) continue; // allow ptr == chunk end
		vm.currentChunkIndex = i;
		vm.ip = chunk->code;
		return 1;
	}
	return 0;
}

Chunk* getCurrentChunk(void) {
	return getChunkByIndex(vm.currentChunkIndex);
}

static void reverseMemcpy(uint8_t* dst, uint8_t* src, size_t len) {
	for (size_t i = 0; i < len; i++) {
		dst[len - 1 - i] = src[i];
	}
}
static void* bytesToPointer(uint8_t* bytes);

void push(Value value) {
	if (vm.stackTop >= vm.stack + STACK_MAX) return; // stack overflow error?
	*vm.stackTop = value;
	vm.stackTop++;
}
void pushArr(Value* values, size_t len) {
	if (vm.stackTop + len > vm.stack + STACK_MAX) return;
	reverseMemcpy(vm.stackTop, values, len);
	vm.stackTop += len;
}
void pushPtr(void* ptr) {
	pushArr(&ptr, ptrSize);
}
static void copy(size_t index, size_t len) {
	if (len == 0) return;
	Value* loc = vm.stackTop - index - len; // technically vm.stackTop - (index + 1) - (len - 1)
	if (vm.stackTop + len > vm.stack + STACK_MAX) return;
	memcpy(vm.stackTop, loc, len);
	vm.stackTop += len;
}
static void move(size_t index, size_t len) {
	if (len == 0 || index == 0) return;
	Value* loc = vm.stackTop - index - len; // technically vm.stackTop - (index + 1) - (len - 1)
	if (vm.stackTop + len > vm.stack + STACK_MAX) return;
	Value* loc2 = loc + len;
	size_t len2 = vm.stackTop - loc2;
	Value* temp = malloc(len);
	memcpy(temp, loc, len);
	memcpy(loc, loc2, len2);
	memcpy(vm.stackTop - len, temp, len);
	free(temp);
}
Value pop(void) {
	if (vm.stackTop <= vm.stack) return NULL;
	vm.stackTop--;
	return *vm.stackTop;
}
Value* popArr(size_t len) {
	if (vm.stackTop - len < vm.stack) return NULL;
	Value* arr = malloc(len);
	reverseMemcpy(arr, vm.stackTop - len, len);
	vm.stackTop -= len;
	return arr;
}
void* popPtr(void) {
	uint8_t* locationBytes = (uint8_t*)popArr(ptrSize);
	void* location = bytesToPointer(locationBytes);
	free(locationBytes);
	return location;
}
uint32_t popInsnIndex(void) {
	return ((uint32_t)pop() << 8 * 3) | ((uint32_t)pop() << 8 * 2) | ((uint32_t)pop() << 8 * 1) | ((uint32_t)pop() << 8 * 0);
}
Value get(size_t index) {
	Value* loc = vm.stackTop - index - 1;
	if (loc < vm.stack) return NULL;
	return *(loc);
}

Value* getArr(size_t index, size_t len) {
	Value* arr = malloc(len);
	Value* loc = vm.stackTop - index - len;
	if (loc < vm.stack) return NULL;
	reverseMemcpy(arr, loc, len);
	return arr;
}

InterpretResult interpret() {
	printf("== Interpreting chunk ==\n");
	return run();
}

static uint8_t* getStructEntry(uint8_t* structPtr, uint8_t entryIndex) {
	uint8_t entryCount = structPtr[0];
	if (entryIndex >= entryCount) return NULL;
	uint8_t entryOffset = structPtr[1 + entryIndex];

	return structPtr + entryOffset;
}
static uint8_t getStructEntrySize(uint8_t* structPtr, uint8_t entryIndex) {
	uint8_t entryCount = structPtr[0];
	if (entryIndex >= entryCount) return NULL;

	uint8_t thisEntryOffset = structPtr[1 + entryIndex];
	uint8_t nextEntryOffset = structPtr[1 + entryIndex + 1]; // last element of entry_sizes is offset to byte after end of struct

	return nextEntryOffset - thisEntryOffset;
}

static int arraysEqual(uint8_t* a, uint8_t* b, size_t len) {
	for (size_t i = 0; i < len; i++) {
		if (a[i] != b[i]) return 0;
	}
	return 1;
}

static size_t footprint(size_t byteAmt, size_t ptrAmt) {
	return byteAmt + (ptrAmt * ptrSize);
}

static InterpretResult run() {
#define READ_BYTE() (*(vm.ip++))
	Chunk* chunk = getCurrentChunk();
	uint8_t* instructionEnd = chunk->code + chunk->count;

	while (vm.ip < instructionEnd) {
		if (vm.debugTrace) {
			printf("   v [ ");
			for (Value* slot = vm.stackTop - 1; slot >= vm.stack; slot--) {
				printValue(*slot);
				if (slot != vm.stack) printf(" ");
			}
			printf(" ] bottom\n");
			disassembleInstruction(chunk, (int)(vm.ip - chunk->code));
		}

		uint8_t instruction;
		switch (instruction = READ_BYTE()) {
		case OP_RETURN: 
			return INTERPRET_OK;
		case OP_DEBUG:
			printf("debug\n");
			break;
		case OP_PRINT: {
			uint8_t byteLen = READ_BYTE();
			uint8_t ptrLen = READ_BYTE();
			size_t len = footprint(byteLen, ptrLen);
			Value* value = popArr(len);

			Value* valueWithTerminator = realloc(value, len + 1);
			if (!valueWithTerminator) return INTERPRET_ERROR;
			value = valueWithTerminator;
			*(value + len) = '\0';

			printf("%s", value);
			free(value);
			break;
		}
		case OP_SCAN: {
			char input = scanChar();
			if (!input) return INTERPRET_ERROR;
			push(input);
			break;
		}
		case OP_TIME: {
			push((Value)(time(NULL) & 0xFF));
			break;
		}
		case OP_PUSH:
			push((Value)READ_BYTE());
			break;
		case OP_PUSH_ALL: {
			uint8_t length = READ_BYTE();
			if (length == 0) {
				for (int i = 0; i < ptrSize; i++) push(0);
				break;
			}

			// reverse bytes before pushing
			uint8_t* bytes = malloc(length);
			if (!bytes) return INTERPRET_ERROR;
			for (int count = 0; count < length; count++) {
				bytes[count] = READ_BYTE();
			}
			for (int count = length - 1; count >= 0; count--) {
				push((Value)bytes[count]);
			}

			free(bytes);
			break;
		}
		case OP_POP: {
			pop();
			break;
		}
		case OP_POP_ALL: {
			uint8_t byteLen = READ_BYTE();
			uint8_t ptrLen = READ_BYTE();
			Value* arr = popArr(footprint(byteLen, ptrLen));
			free(arr);
			break;
		}
		case OP_PUSH_THIS:
			printf("push_this not implemented");
			break;
		case OP_PUSH_UNIT: {
			pushPtr(&(chunk->unit));
			break;
		}
		case OP_PUSH_UNIT_OF: 
			printf("push_unit_of not implemented");
			break;
		case OP_PUSH_PTR_NEXT:
			pushPtr(vm.ip);
			break;
		case OP_PUSH_PTR_ARR:
			pushPtr(vm.ip);
			uint8_t length = READ_BYTE();
			vm.ip += length;
			if (vm.ip >= instructionEnd) return INTERPRET_ERROR;
			break;
		case OP_PUSH_PTR: {
			uint32_t index = 0;
			index = (index + READ_BYTE()) << 8 * 3;
			index = (index + READ_BYTE()) << 8 * 2;
			index = (index + READ_BYTE()) << 8 * 1;
			index = (index + READ_BYTE());
			void* dest = chunk->code + index;
			if (dest > instructionEnd) return INTERPRET_ERROR; // allow dest == instructionEnd
			pushPtr(dest);
			break;
		}
		case OP_COPY: {
			uint8_t byteOffset = READ_BYTE();
			uint8_t ptrOffset = READ_BYTE();
			uint8_t byteLen = READ_BYTE();
			uint8_t ptrLen = READ_BYTE();

			copy(footprint(byteOffset, ptrOffset), footprint(byteLen, ptrLen));
			break;
		}
		case OP_MOVE: {
			uint8_t byteOffset = READ_BYTE();
			uint8_t ptrOffset = READ_BYTE();
			uint8_t byteLen = READ_BYTE();
			uint8_t ptrLen = READ_BYTE();

			move(footprint(byteOffset, ptrOffset), footprint(byteLen, ptrLen));
			break;
		}
		case OP_SET: {
			uint8_t byteLen = READ_BYTE();
			uint8_t ptrLen = READ_BYTE();
			uint8_t byteOffset = READ_BYTE();
			uint8_t ptrOffset = READ_BYTE();

			size_t offset = footprint(byteOffset, ptrOffset);
			size_t len = footprint(byteLen, ptrLen);
			Value* src = vm.stackTop - len;
			Value* dest = src - offset;

			if (dest < vm.stack) return INTERPRET_ERROR;
			memcpy(dest, src, len);
			vm.stackTop = vm.stackTop - min(offset, len);
			break;
		}
		case OP_ADD: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			if (!a || !b) return INTERPRET_ERROR;

			uint16_t buffer = 0; // buffer is 2 bytes to fit the carry bit when adding byte-by-byte right to left
			for (uint8_t index = 0; index < aLen; index++) {
				buffer += (uint16_t)a[aLen - 1 - index];
				if (index < bLen) buffer += (uint16_t)b[bLen - 1 - index];
				push((Value)(buffer & 0xFF));
				buffer = buffer >> 8;
			}

			free(a);
			free(b);
			break;
		}
		case OP_MULTIPLY: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			Value* c = malloc(aLen);
			if (!a || !b || !c) return INTERPRET_ERROR;

			memset(c, 0, aLen);

			for (uint8_t aIndex = 0; aIndex < aLen; aIndex++) {
				uint16_t carry = 0; // carry is 2 bytes to fit the carryover (even if it is 0xFF * 0xFF it will still fit in 2 bytes)
				for (uint8_t bIndex = 0; bIndex < aLen - aIndex; bIndex++) {
					uint16_t product = a[aLen - 1 - aIndex] * b[bLen - 1 - bIndex] + carry;
					c[aLen - 1 - aIndex - bIndex] += (Value)(product & 0xFF);
					carry = product >> 8;
				}
			}

			pushArr(c, aLen);

			free(a);
			free(b);
			free(c);
			break;
		}
		case OP_OR: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			if (!a || !b) return INTERPRET_ERROR;

			for (uint8_t index = 0; index < aLen; index++) {
				uint8_t buffer = a[aLen - 1 - index];
				if (index < bLen) buffer |= b[bLen - 1 - index];
				push((Value)buffer);
			}

			free(a);
			free(b);
			break;
		}
		case OP_AND: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			if (!a || !b) return INTERPRET_ERROR;

			for (uint8_t index = 0; index < aLen; index++) {
				uint8_t buffer = a[aLen - 1 - index];
				if (index < bLen) buffer &= b[bLen - 1 - index];
				push((Value)buffer);
			}

			free(a);
			free(b);
			break;
		}
		case OP_XOR: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			if (!a || !b) return INTERPRET_ERROR;

			for (uint8_t index = 0; index < aLen; index++) {
				uint8_t buffer = a[aLen - 1 - index];
				if (index < bLen) buffer ^= b[bLen - 1 - index];
				push((Value)buffer);
			}

			free(a);
			free(b);
			break;
		}
		case OP_LSHIFT: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);

			Value* a = popArr(aLen);
			Value b = pop();
			if (!a || !b) return INTERPRET_ERROR;

			for (uint8_t index = 0; index < aLen; index++) {
				uint16_t buffer = (uint16_t)a[index] << 8;
				if (index < aLen - 1) buffer += a[index + 1];
				buffer <<= b;
				buffer >>= 8;
				push((Value)(buffer & 0xFF));
			}

			free(a);
			break;
		}
		case OP_RSHIFT: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);

			Value* a = popArr(aLen);
			Value b = pop();
			if (!a || !b) return INTERPRET_ERROR;

			for (uint8_t index = 0; index < aLen; index++) {
				uint16_t buffer = (uint16_t)a[index];
				if (index > 0) buffer += a[index - 1] << 8;
				buffer >>= b;
				push((Value)(buffer & 0xFF));
			}

			free(a);
			break;
		}
		case OP_EQUALS: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			if (!a || !b) return INTERPRET_ERROR;
			
			uint8_t result = 0;

			for (uint8_t index = 0; index < aLen || index < bLen; index++) {
				if (index < aLen) {
					if (index < bLen) {
						result = (a[aLen - 1 - index] == b[bLen - 1 - index]);
					} else {
						result = (a[aLen - 1 - index] == 0);
					}
				} else {
					result = (b[bLen - 1 - index] == 0);
				}
				if (!result) break;
			}

			push((Value)result);
			free(a);
			free(b);
			break;
		}
		case OP_GREATER: {
			uint8_t aByteLen = READ_BYTE();
			uint8_t aPtrLen = READ_BYTE();
			uint8_t bByteLen = READ_BYTE();
			uint8_t bPtrLen = READ_BYTE();

			size_t aLen = footprint(aByteLen, aPtrLen);
			size_t bLen = footprint(bByteLen, bPtrLen);

			Value* a = popArr(aLen);
			Value* b = popArr(bLen);
			if (!a || !b) return INTERPRET_ERROR;

			uint8_t result = 0;

			size_t aOff = max(0, bLen - aLen);
			size_t bOff = max(0, aLen - bLen);

			// this comparison is unsigned, so the cases where 0's are prepended can be ignored
			for (uint8_t index = 0; index < aLen || index < bLen; index++) {
				if (index >= aOff && index >= bOff) {
					Value aByte = a[index - aOff];
					Value bByte = b[index - bOff];

					if (index < aLen - 1) result = aByte >= bByte; // >= is okay for every byte before the last
					else result = aByte > bByte;
					if (!result) break;
				}
			}

			push((Value)result);
			free(a);
			free(b);
			break;
		}
		case OP_MALLOC: {
			uint8_t len = (uint8_t)pop();

			pushPtr(malloc(len)); // rely on Monarch program to not lose this

			break;
		}
		case OP_MSET: {
			uint8_t len = (uint8_t)pop();
			uint8_t* value = (uint8_t*)popArr(len);
			void* location = popPtr();

			memcpy(location, value, len);

			free(value);
			break;
		}
		case OP_MGET: {
			uint8_t len = (uint8_t)pop();
			uint8_t* value = malloc(len);
			if (!value) return INTERPRET_ERROR;
			void* location = popPtr();

			memcpy(value, location, len);
			pushArr(value, len);

			free(len);
			break;
		}
		case OP_FREE: {
			void* location = popPtr();

			free(location); // may do damage if this is not a pointer allocated by malloc in OP_MALLOC

			break;
		}
		case OP_JUMP_TO_INDEX: {
			vm.ip = chunk->code + popInsnIndex();
			if (vm.ip >= instructionEnd) return INTERPRET_ERROR;
			break;
		}
		case OP_JUMP_FORWARD: {
			uint8_t offset = (uint8_t)pop();
			uint8_t* targetIndex = vm.ip-1 + offset;
			if (targetIndex >= instructionEnd) return INTERPRET_ERROR;
			vm.ip = targetIndex;
			break;
		}
		case OP_JUMP_BACKWARD: {
			uint8_t offset = (uint8_t)pop();
			uint8_t* targetIndex = vm.ip-1 - offset;
			if (targetIndex >= instructionEnd) return INTERPRET_ERROR;
			vm.ip = targetIndex;
			break;
		}
		case OP_JUMP_IF: {
			uint32_t targetIndex = popInsnIndex();
			uint8_t condition = (uint8_t)pop();

			if (condition != 0) vm.ip = chunk->code + targetIndex;

			break;
		}
		/*case OP_JUMP_IF_EQUAL: {
			uint8_t compareLen = (uint8_t)pop();
			uint8_t* targetIndex = (uint8_t)pop(); // no checks because vm.ip is currently also only 1 byte
			uint8_t* a = (uint8_t*)getArr(0, compareLen);
			uint8_t* b = (uint8_t*)getArr(compareLen, compareLen);

			int comparison = memcmp(a, b, compareLen);
			if (comparison == 0) vm.ip = targetIndex;

			free(a);
			free(b);
			break;
		}*/
		case OP_JUMP_ARR_SWITCH: {
			uint8_t branchCount = READ_BYTE();
			if (branchCount == 0) return INTERPRET_ERROR;

			uint8_t* subjectPtr = (uint8_t*)popPtr();
			if (!subjectPtr) return INTERPRET_ERROR;
			uint8_t subjectLen = *subjectPtr;
			uint8_t* subject = subjectPtr + 1;

			uint8_t** keys = malloc(sizeof(uint8_t*) * branchCount);
			uint8_t* keyLens = malloc(sizeof(uint8_t) * branchCount);
			if (!keys || !keyLens) return INTERPRET_ERROR;
			uint8_t* keyOffset = vm.ip + branchCount * sizeof(uint32_t);
			for (int keyIndex = 0; keyIndex < branchCount; keyIndex++) {
				if (keyOffset >= instructionEnd) return INTERPRET_ERROR;
				// get key len
				size_t keyLen = *keyOffset;
				keyLens[keyIndex] = keyLen;

				// create key array and copy key into it
				keys[keyIndex] = malloc(sizeof(uint8_t) * keyLen);
				if (!keys[keyIndex]) return INTERPRET_ERROR;
				memcpy(keys[keyIndex], keyOffset + 1, keyLen);

				keyOffset += keyLen + 1;
			}

			int matched = 0;
			// test subject against all keys
			for (int index = 0; index < branchCount; index++) {
				if (subjectLen != keyLens[index]) continue;
				if (!arraysEqual(subject, keys[index], subjectLen)) continue;

				uint32_t branch = (vm.ip[index * sizeof(uint32_t)] << 8 * 3) |
					(vm.ip[index * sizeof(uint32_t) + 1] << 8 * 2) |
					(vm.ip[index * sizeof(uint32_t) + 2] << 8 * 1) |
					(vm.ip[index * sizeof(uint32_t) + 3]);
				vm.ip = chunk->code + branch;
				if (vm.ip >= instructionEnd) return INTERPRET_ERROR;
				matched = 1;
				break;
			}

			if (!matched) return INTERPRET_ERROR;

			for (int i = 0; i < branchCount; i++)
				free(keys[i]);
			free(keys);
			free(keyLens);
			break;
		}
		case OP_JUMP_SWITCH: {
			uint8_t branchCount = READ_BYTE();
			uint8_t branchIndex = (uint8_t)pop();
			if (branchIndex >= branchCount) return INTERPRET_ERROR;

			uint32_t branch = (vm.ip[branchIndex] << 8 * 3) | 
				(vm.ip[branchIndex + 1] << 8 * 2) | 
				(vm.ip[branchIndex + 2] << 8 * 1) | 
				(vm.ip[branchIndex + 3]);
			vm.ip = chunk->code + branch;
			if (vm.ip >= instructionEnd) return INTERPRET_ERROR;
			break;
		}
		case OP_JUMP_TO_UNIT: {
			uint8_t* namePtr = popPtr();
			uint8_t nameLen = namePtr[0];
			
			char* name = malloc((size_t)nameLen + 1);
			if (!name) return INTERPRET_ERROR;
			memcpy(name, namePtr + 1, nameLen);
			name[nameLen] = '\0';

			if (!getChunkByName(name)) {
				if (!readAndAddChunk(name)) return INTERPRET_ERROR;
			}
			setCurrentChunkByName(name);

			free(name);
			break;
		}
		case OP_JUMP_TO_PTR: {
			void* dest = popPtr();
			if (!setCurrentChunkByPtr(dest)) return INTERPRET_ERROR;
			vm.ip = dest;
			break;
		}
		case OP_STRUCT_CREATE: {
			uint8_t entryCount = (uint8_t)pop();
			uint8_t* entrySizes = popArr(entryCount);
			if (!entrySizes) return INTERPRET_ERROR;
			uint8_t totalEntrySize = 0;
			for (size_t i = 0; i < entryCount; i++) {
				uint8_t entrySize = entrySizes[i];
				if (entrySize == 0) entrySize = ptrSize;
				if (totalEntrySize + entrySize < totalEntrySize) return INTERPRET_ERROR;
				totalEntrySize += entrySize;
			}

			// struct geography:
			// {1:entry_count,entry_count+1:entry_offsets,?:entries}
			// last element of entry_offsets is the offset from the start to the end of entries

			uint8_t totalStructSize = 1 + entryCount + 1 + totalEntrySize;
			uint8_t* structPtr = (uint8_t*)malloc(totalStructSize);
			if (!structPtr) return INTERPRET_ERROR;
			
			// init struct
			structPtr[0] = entryCount;
			for (uint8_t entryIndex = 0, offset = 1 + entryCount + 1; entryIndex < entryCount; entryIndex++) {
				structPtr[1 + entryIndex] = offset;

				uint8_t entrySize = entrySizes[entryIndex];
				if (entrySize == 0) entrySize = ptrSize;
				offset += entrySize;
			}
			structPtr[1 + entryCount] = totalStructSize;
			for (uint8_t entryByteOffset = 0; entryByteOffset < totalEntrySize; entryByteOffset++) {
				structPtr[1 + entryCount + 1 + entryByteOffset] = 0;
			}

			// push ptr to struct
			pushPtr(structPtr);

			free(entrySizes);
			break;
		}
		case OP_STRUCT_ENTRY: {
			uint8_t entryIndex = READ_BYTE();
			uint8_t* structPtr = (uint8_t*)popPtr();
			if (!structPtr) return INTERPRET_ERROR;

			uint8_t* entryPtr = getStructEntry(structPtr, entryIndex);
			if (!entryPtr) return INTERPRET_ERROR;
			pushPtr(entryPtr);

			break;
		}
		case OP_STRUCT_ENTRY_SIZE: {
			uint8_t entryIndex = READ_BYTE();
			uint8_t* structPtr = (uint8_t*)popPtr();
			if (!structPtr) return INTERPRET_ERROR;

			uint8_t entrySize = getStructEntrySize(structPtr, entryIndex);
			if (!entrySize) return INTERPRET_ERROR;
			push((Value)entrySize);

			break;
		}
		case OP_STRUCT_SET: {
			uint8_t entryIndex = READ_BYTE();
			uint8_t* structPtr = (uint8_t*)popPtr();
			if (!structPtr) return INTERPRET_ERROR;
			uint8_t* entryPtr = getStructEntry(structPtr, entryIndex);
			uint8_t entrySize = getStructEntrySize(structPtr, entryIndex);
			if (entrySize == 0) entrySize = ptrSize;
			if (!entryPtr || !entrySize) return INTERPRET_ERROR;
			uint8_t* value = (uint8_t*)popArr(entrySize);
			if (!value) return INTERPRET_ERROR;

			memcpy(entryPtr, value, entrySize);

			free(value);
			break;
		}
		case OP_STRUCT_GET: {
			uint8_t entryIndex = READ_BYTE();
			uint8_t* structPtr = (uint8_t*)popPtr();
			if (!structPtr) return INTERPRET_ERROR;
			uint8_t* entryPtr = getStructEntry(structPtr, entryIndex);
			uint8_t entrySize = getStructEntrySize(structPtr, entryIndex);
			if (entrySize == 0) entrySize = ptrSize;
			if (!entryPtr || !entrySize) return INTERPRET_ERROR;

			pushArr(entryPtr, entrySize);

			break;
		}
		default:
			break;
		}

		chunk = getCurrentChunk();
		instructionEnd = chunk->code + chunk->count;
	}
	return INTERPRET_OK;

#undef READ_BYTE
}

static void* bytesToPointer(uint8_t* bytes) {
	void* ptr;
	memcpy(&ptr, bytes, ptrSize);
	return ptr;
}
