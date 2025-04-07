#include <string.h>
#include <stdlib.h>

char* strDyn(char* s) {
	char* p = malloc(strlen(s) + 1);
	if (!p) return NULL;
	strcpy_s(p, strlen(s) + 1, s);
	return p;
}

char* strConcat(char* a, char* b) {
	size_t aLen = strlen(a), bLen = strlen(b);
	char* result = malloc(aLen + bLen + 1);
	if (!result) return NULL;
	memcpy(result, a, aLen);
	memcpy(result + aLen, b, bLen);
	result[aLen + bLen] = '\0';
	return result;
}