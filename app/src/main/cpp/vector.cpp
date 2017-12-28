#include <stdio.h>
#include <stdlib.h>
#include "vector.h"

void Vector::vector_init() {
    // 初始化 size 和 capacity。
    size = 0;
    capacity = VECTOR_INITIAL_CAPACITY;
    // 为 vector 内部 data 数组对象申请内存空间
    data = (long *) malloc(sizeof(long) * capacity);
}

void Vector::vector_append(long value) {
    // 确保当前有足够的内存空间可用。
    vector_double_capacity_if_full();
    // 将整数追加到数组尾部。
    data[size++] = value;
}

int Vector::vector_get(int index) {
    if (index >= size || index < 0) {
        printf("Index %d out of bounds for vector of size %d\n", index, size);
        exit(1);
    }
    return data[index];
}

void Vector::vector_set(int index, long value) {
    // 使用 0 填充闲置在用内存空间。
    while (index >= size) {
        vector_append(0);
    }
    // 在指定数组位置保存指定整数。
    data[index] = value;
}

void Vector::vector_double_capacity_if_full() {
    if (size >= capacity) {
        // 翻倍数组大小。
        capacity *= 2;
        data = (long *) realloc(data, sizeof(long) * capacity);
    }
}

void Vector::vector_free() {
    free(data);
}