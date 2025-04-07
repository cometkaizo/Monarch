#include <stdio.h>
#include <stdlib.h>

char* readString(FILE* f, size_t size) {
    //The size is extended by the input with the value of the provisional
    char* str;
    int ch;
    size_t len = 0;

    str = malloc(sizeof(*str) * size); //size is start size
    if (!str) return NULL;

    while ((ch = fgetc(f)) != EOF && ch != '\n') {
        str[len++] = ch;
        if (len == size) {
            str = realloc(str, sizeof(*str) * (size += 16));
            if (!str) return NULL;
        }
    }
    str[len++] = '\0';

    return realloc(str, sizeof(*str) * len);
}