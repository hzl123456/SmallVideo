#ifndef SMALLVIDEO_VECTOR_H
#define SMALLVIDEO_VECTOR_H

// 首先定义一个常量，该常量表示 Vector 内部一个数组对象的初始大小。
#define VECTOR_INITIAL_CAPACITY 450

class Vector {

public:
    int size;               // 数组在用长度
    int capacity;           // 数组最大可用长度
    long *data;              // 用来保存整数对象的数组对象

    // 该函数负责初始化一个 Vector 对象，初始数组在用长度为 0，最大长度为 VECTOR_INITIAL_CAPACITY。
    // 开辟适当的内存空间以供底层数组使用，空间大小为 vector->capacity * sizeof(int) 个字节。
    void vector_init();

    // 该函数负责追加整数型的成员到 vector 对象。如果底层的数组已满，则扩大底层数组容积来保存新成员。
    void vector_append(long value);

    // 返回 vector 指定位置所保存的值。如果指定位置小于 0 或者大于 vector->size - 1，则返回异常。
    int vector_get(int index);

    // 将指定值保存到指定位置，如果指定位置大于 vector->size，则自动翻倍 vector 内部的数组容积直到可以容纳指定多的位置。
    // 扩大的数组中间使用 0 填满那些空位置。
    void vector_set(int index, long value);

    // 将 vector 内部数组容积翻倍。
    // 因为更改数组体积的开销是十分大的，采用翻倍的策略以免频繁更改数组体积。
    void vector_double_capacity_if_full();

    // 释放 vector 内部数组所使用的内存空间。
    void vector_free();
};

#endif //SMALLVIDEO_VECTOR_H
