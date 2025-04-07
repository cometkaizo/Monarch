#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "debug.h"

void disassembleChunk(Chunk* chunk, const char* name) {
	printf("== %s == (%d bytes)\n", name, chunk->count);

	for (int offset = 0; offset < chunk->count;) {
		offset = disassembleInstruction(chunk, offset);
	}
}

int disassembleInstruction(Chunk* chunk, int offset) {
	printf("%04x ", offset);

	uint8_t instruction = chunk->code[offset];
	switch (instruction) {
	case OP_RETURN:
		return simpleInstruction("RETURN", offset);
	case OP_DEBUG:
		return simpleInstruction("DEBUG", offset);
	case OP_PRINT:
		return sizeInstruction("PRINT", chunk, offset);
	case OP_SCAN:
		return simpleInstruction("SCAN", offset);
	case OP_TIME:
		return simpleInstruction("TIME", offset);
	case OP_PUSH:
		return byteInstruction("PUSH", chunk, offset);
	case OP_PUSH_ALL:
		return byteArrayInstruction("PUSH_ALL", chunk, offset);
	case OP_POP:
		return simpleInstruction("POP", offset);
	case OP_POP_ALL:
		return sizeInstruction("POP_ALL", chunk, offset);
	case OP_PUSH_THIS:
		return simpleInstruction("PUSH_THIS", offset);
	case OP_PUSH_UNIT:
		return simpleInstruction("PUSH_UNIT", offset);
	case OP_PUSH_UNIT_OF:
		return simpleInstruction("PUSH_UNIT_OF", offset);
	case OP_PUSH_PTR_NEXT:
		return simpleInstruction("PUSH_PTR_NEXT", offset);
	case OP_PUSH_PTR_ARR:
		return byteArrayInstruction("PUSH_PTR_ARR", chunk, offset);
	case OP_PUSH_PTR:
		return insnIndexInstruction("PUSH_PTR", chunk, offset);
	case OP_COPY:
		return offsetAndSizeInstruction("COPY", chunk, offset);
	case OP_MOVE:
		return offsetAndSizeInstruction("MOVE", chunk, offset);
	case OP_SET:
		return sizeAndOffsetInstruction("SET", chunk, offset);
	case OP_ADD:
		return sizeAndSizeInstruction("ADD", chunk, offset);
	case OP_MULTIPLY:
		return sizeAndSizeInstruction("MULTIPLY", chunk, offset);
	case OP_OR:
		return sizeAndSizeInstruction("OR", chunk, offset);
	case OP_AND:
		return sizeAndSizeInstruction("AND", chunk, offset);
	case OP_XOR:
		return sizeAndSizeInstruction("XOR", chunk, offset);
	case OP_LSHIFT:
		return sizeInstruction("LSHIFT", chunk, offset);
	case OP_RSHIFT:
		return sizeInstruction("RSHIFT", chunk, offset);
	case OP_EQUALS:
		return sizeAndSizeInstruction("EQUALS", chunk, offset);
	case OP_GREATER:
		return sizeAndSizeInstruction("GREATER", chunk, offset);
	case OP_MALLOC:
		return simpleInstruction("MALLOC", offset);
	case OP_MSET:
		return simpleInstruction("MSET", offset);
	case OP_MGET:
		return simpleInstruction("MGET", offset);
	case OP_FREE:
		return simpleInstruction("FREE", offset);
	case OP_JUMP_TO_INDEX:
		return simpleInstruction("JUMP_TO_INDEX", offset);
	case OP_JUMP_FORWARD:
		return simpleInstruction("JUMP_FORWARD", offset);
	case OP_JUMP_BACKWARD:
		return simpleInstruction("JUMP_BACKWARD", offset);
	case OP_JUMP_IF:
		return simpleInstruction("JUMP_IF", offset);
	case OP_JUMP_ARR_SWITCH:
		return jumpArrSwitchInstruction("JUMP_ARR_SWITCH", chunk, offset);
	case OP_JUMP_SWITCH:
		return jumpSwitchInstruction("JUMP_SWITCH", chunk, offset);
	case OP_JUMP_TO_UNIT:
		return simpleInstruction("JUMP_TO_UNIT", offset);
	case OP_JUMP_TO_PTR:
		return simpleInstruction("JUMP_TO_PTR", offset);
	case OP_STRUCT_CREATE:
		return simpleInstruction("STRUCT_CREATE", offset);
	case OP_STRUCT_ENTRY:
		return simpleInstruction("STRUCT_ENTRY", offset);
	case OP_STRUCT_ENTRY_SIZE:
		return simpleInstruction("STRUCT_ENTRY_SIZE", offset);
	case OP_STRUCT_SET:
		return structEntryAccessInstruction("STRUCT_SET", chunk, offset);
	case OP_STRUCT_GET:
		return structEntryAccessInstruction("STRUCT_GET", chunk, offset);
	default:
		printf("Unknown opcode %d\n", instruction);
		return offset + 1;
	}
	return 0;
}

static int simpleInstruction(const char* name, int offset) {
	printf("%s\n", name);
	return offset+1;
}
static int byteInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t value = chunk->code[offset + 1];

	printf("%s: %02x\n", name, value);
	return offset+1 + 1;
}
static int insnIndexInstruction(const char* name, Chunk* chunk, int offset) {
	printf("%s: %02x%02x%02x%02x\n", name, 
		*(chunk->code + offset + 1), 
		*(chunk->code + offset + 2), 
		*(chunk->code + offset + 3), 
		*(chunk->code + offset + 4));
	return offset + 1 + 4;
}
static int sizeInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t byteAmt = chunk->code[offset + 1];
	uint8_t ptrAmt = chunk->code[offset + 2];

	printf("%s (len: %db + %dp)\n", name, byteAmt, ptrAmt);
	return offset + 1 + 2;
}
static int byteArrayInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t length = chunk->code[offset + 1];

	printf("%s (%db): ", name, length);
	for (int index = 0; index < length; index++) {
		printf("%02x ", chunk->code[offset + 2 + index]);
	}
	printf("\n");
	return offset+1 + 1+length;
}
static int offsetAndSizeInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t byteOffset = chunk->code[offset + 1];
	uint8_t ptrOffset = chunk->code[offset + 2];
	uint8_t byteSize = chunk->code[offset + 3];
	uint8_t ptrSize = chunk->code[offset + 4];

	printf("%s (offset: %db + %dp) (len: %db + %dp)\n", name, byteOffset, ptrOffset, byteSize, ptrSize);
	return offset+1 + 4;
}
static int sizeAndOffsetInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t byteSize = chunk->code[offset + 1];
	uint8_t ptrSize = chunk->code[offset + 2];
	uint8_t byteOffset = chunk->code[offset + 3];
	uint8_t ptrOffset = chunk->code[offset + 4];

	printf("%s (len: %db + %dp) (offset: %db + %dp)\n", name, byteSize, ptrSize, byteOffset, ptrOffset);
	return offset + 1 + 4;
}
static int sizeAndSizeInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t aByteSize = chunk->code[offset + 1];
	uint8_t aPtrSize = chunk->code[offset + 2];
	uint8_t bByteSize = chunk->code[offset + 3];
	uint8_t bPtrSize = chunk->code[offset + 4];

	printf("%s (len: %db + %dp) (len: %db + %dp)\n", name, aByteSize, aPtrSize, bByteSize, bPtrSize);
	return offset + 1 + 4;
}
static int jumpArrSwitchInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t branchCount = chunk->code[offset + 1];

	uint8_t** keys = malloc(sizeof(uint8_t*) * branchCount);
	uint8_t* keyLens = malloc(sizeof(uint8_t) * branchCount);
	int keyOffset = offset + 1 + 1 + branchCount * sizeof(uint32_t);
	if (keys == NULL || keyLens == NULL) return -1;
	for (int keyIndex = 0; keyIndex < branchCount; keyIndex++) {
		// get key len
		size_t keyLen = chunk->code[keyOffset];
		keyLens[keyIndex] = keyLen;

		// create key array and copy key into it
		keys[keyIndex] = malloc(sizeof(uint8_t) * keyLen);
		if (keys[keyIndex] == NULL) return -1;
		memcpy(keys[keyIndex], chunk->code + keyOffset + 1, keyLen);

		keyOffset += keyLen + 1;
	}

	printf("%s (%d branches):  ", name, branchCount);
	for (int index = 0; index < branchCount; index++) {
		printf("%.*s:%02x%02x%02x%02x  ", keyLens[index], keys[index], 
			*(chunk->code + offset + 2 + index * sizeof(uint32_t)), 
			*(chunk->code + offset + 3 + index * sizeof(uint32_t)), 
			*(chunk->code + offset + 4 + index * sizeof(uint32_t)), 
			*(chunk->code + offset + 5 + index * sizeof(uint32_t)));
	}
	printf("\n");

	for (int i = 0; i < branchCount; i++)
		free(keys[i]);
	free(keys);
	return keyOffset;
}
static int jumpSwitchInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t branchCount = chunk->code[offset + 1];

	printf("%s (%d branches):  ", name, branchCount);
	for (int index = 0; index < branchCount; index++) {
		printf("%02x%02x%02x%02x  ", 
			*(chunk->code + offset + 2 + index * sizeof(uint32_t)), 
			*(chunk->code + offset + 3 + index * sizeof(uint32_t)), 
			*(chunk->code + offset + 4 + index * sizeof(uint32_t)), 
			*(chunk->code + offset + 5 + index * sizeof(uint32_t)));
	}
	printf("\n");
	return offset+1 + 1+branchCount;
}
static int structEntryAccessInstruction(const char* name, Chunk* chunk, int offset) {
	uint8_t value = chunk->code[offset + 1];

	printf("%s (%dth entry)\n", name, value);
	return offset+1 + 1;
}
