#pragma once

#include <stdio.h>
#include "common.h"

typedef enum {
	OP_RETURN,
	OP_DEBUG,
	OP_PRINT,
	OP_SCAN,
	OP_TIME,
	OP_PUSH,
	OP_PUSH_ALL,
	OP_POP,
	OP_POP_ALL,
	OP_PUSH_THIS,
	OP_PUSH_UNIT,
	OP_PUSH_UNIT_OF,
	OP_PUSH_PTR_NEXT,
	OP_PUSH_PTR_ARR,
	OP_PUSH_PTR_STACK,
	OP_PUSH_PTR,
	OP_COPY,
	OP_MOVE,
	OP_SET,
	OP_ADD,
	OP_SUBTRACT,
	OP_MULTIPLY,
	OP_DIVIDE,
	OP_MODULO,
	OP_OR,
	OP_AND,
	OP_XOR,
	OP_LSHIFT,
	OP_RSHIFT,
	OP_EQUALS,
	OP_GREATER,
	OP_MALLOC,
	OP_MSET,
	OP_MGET,
	OP_FREE,
	OP_JUMP_TO_INDEX,
	OP_JUMP_FORWARD,
	OP_JUMP_BACKWARD,
	OP_JUMP_IF,
	OP_JUMP_ARR_SWITCH,
	OP_JUMP_SWITCH,
	OP_JUMP_TO_UNIT,
	OP_JUMP_TO_PTR,
	OP_STRUCT_CREATE,
	OP_STRUCT_ENTRY,
	OP_STRUCT_ENTRY_SIZE,
	OP_STRUCT_SET,
	OP_STRUCT_GET,
	OP_DEBUG_FLAG,
} Opcode;

typedef struct {
	char* name;
	uint8_t nameLen;
	int count;
	int capacity;
	uint8_t* code;
	void* unit;
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void addToChunk(Chunk* chunk, uint8_t byte);
Chunk* readChunk(char* basePath, char* name);