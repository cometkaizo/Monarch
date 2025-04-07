#pragma once

#include "chunk.h"
#include "value.h"

#define STACK_MAX 256

typedef struct {
	char* basePath;
	Chunk** chunks;
	uint8_t chunkCount;
	uint8_t currentChunkIndex;
	uint8_t* ip; // the Instruction Pointer to the next insn to be executed

	Value stack[STACK_MAX];
	Value* stackTop; // stack location + length of stack

	char* input;
	char* inputEnd;
	char* inputCursor; // points to the next input that is to be scanned

	uint8_t debugTrace; // boolean - if true, prints the instruction traces
} VM;

typedef enum {
	INTERPRET_OK,
	INTERPRET_ERROR
} InterpretResult;

void initVM(char* basePath);
void setVMDebugTrace(int debugTrace);
void freeVM(void);
int addChunk(Chunk* chunk);
int readAndAddChunk(char* name);
Chunk* getChunkByIndex(uint8_t index);
Chunk* getChunkByName(char* name);
int setCurrentChunkByName(char* name);
int setCurrentChunkByPtr(void* ptr);
Chunk* getCurrentChunk(void);
char scanChar(void);
InterpretResult interpret();
void push(Value value);
void pushArr(Value* values, size_t len);
void pushPtr(void* ptr);
Value pop(void);
Value* popArr(size_t len);
void* popPtr(void);
uint32_t popInsnIndex(void);
Value get(size_t index);
Value* getArr(size_t index, size_t len);
