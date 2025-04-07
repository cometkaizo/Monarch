#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "chunk.h"
#include "memory.h"
#include "strutil.h"

void initChunk(Chunk* chunk) {
	chunk->name = NULL;
	chunk->count = 0;
	chunk->capacity = 0;
	chunk->code = NULL;
	chunk->unit = NULL;
}

void freeChunk(Chunk* chunk) {
	FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
	initChunk(chunk);
}

void addToChunk(Chunk* chunk, uint8_t byte) {
	if (chunk->count >= chunk->capacity) {
		int oldCapacity = chunk->capacity;
		chunk->capacity = GROW_CAPACITY(oldCapacity);
		chunk->code = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
	}

	chunk->code[chunk->count] = byte;
	chunk->count++;
}

Chunk* readChunk(char* basePath, char* name) {
	Chunk* chunk = malloc(sizeof(Chunk));
	if (!chunk) return NULL;
	initChunk(chunk);

	chunk->name = _strdup(name);

	char* fullPath = strConcat(basePath, name);
	if (!fullPath) return NULL;
	FILE* file = fopen(fullPath, "rb");
	free(fullPath);
	if (!file) return NULL;

	long size;
	fseek(file, 0, SEEK_END);
	size = ftell(file);
	rewind(file);

	chunk->code = malloc((size_t)size + 1);
	if (!chunk->code) return NULL;

	fread(chunk->code, size, 1, file);

	chunk->capacity = size;
	chunk->count = chunk->capacity;

	fclose(file);
	return chunk;
}