#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "memory.h"

size_t ptrSize = sizeof(void*);

void* reallocate(void* pointer, size_t oldSize, size_t newSize) {
	if (newSize == 0) {
		free(pointer);
		return NULL;
	}

	void* result = realloc(pointer, newSize);
	if (result == NULL) exit(1);
	return result;
}

int isBigEndian(void) {
	static int bigEndian = -1;
	if (bigEndian == -1) {
		int num = 1;
		uint8_t* ptr = (uint8_t*)&num;
		bigEndian = *ptr == 0;
	}
	return bigEndian;
}
void reverseMemcpy(uint8_t* dst, uint8_t* src, size_t len) {
	for (size_t i = 0; i < len; i++) {
		dst[len - 1 - i] = src[i];
	}
}
void memcpyWithSystem(uint8_t* dst, uint8_t* src, size_t len) {
	// Monarch does arithmetic etc. in Big-Endian, but OS might be Little-Endian
	if (isBigEndian()) memcpy(dst, src, len);
	else reverseMemcpy(dst, src, len);
}
void* bytesToPointer(uint8_t* bytes) {
	void* ptr;
	memcpy(&ptr, bytes, ptrSize);
	return ptr;
}

void printHexBigEndian(uint32_t num) {
	printf("%d : ", num);
	printf("%02x%02x%02x%02x", (uint8_t)(num >> 8 * 3), (uint8_t)(num >> 8 * 2), (uint8_t)(num >> 8), (uint8_t)num);
}

void printHexBigEndianPtr(uint8_t* num, size_t len) {
	for (size_t i = 0; i < len; i++) {
		printf("%02x", num[i]);
	}
}

void printHexBigEndianPtrSystem(uint8_t* num, size_t len) {
	int bigEndian = isBigEndian();
	for (size_t i = 0; i < len; i++) {
		size_t index = bigEndian ? i : len - i - 1;
		printf("%02x", num[index]);
	}
}
