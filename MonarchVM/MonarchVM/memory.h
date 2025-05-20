#pragma once

#include "common.h"

#define GROW_CAPACITY(capacity) \
	((capacity) < 8? 8 : (capacity) * 2)

#define GROW_ARRAY(type, pointer, oldCount, newCount) \
	(type*)reallocate(pointer, sizeof(type) * (oldCount), \
		sizeof(type) * (newCount))

#define FREE_ARRAY(type, pointer, oldCount) \
	reallocate(pointer, sizeof(type) * (oldCount), 0)

extern size_t ptrSize;

void* reallocate(void* pointer, size_t oldSize, size_t newSize);

int isBigEndian(void);
void reverseMemcpy(uint8_t* dst, uint8_t* src, size_t len);
void memcpyWithSystem(uint8_t* dst, uint8_t* src, size_t len);
void* bytesToPointer(uint8_t* bytes);

void printHexBigEndian(uint32_t num);
void printHexBigEndianPtr(uint8_t* num, size_t len);
void printHexBigEndianPtrSystem(uint8_t* num, size_t len);