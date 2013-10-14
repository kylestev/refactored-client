package com.runescape.net;

import java.math.BigInteger;

import com.runescape.collection.CacheableNode;
import com.runescape.collection.LinkedList;

public class Buffer extends CacheableNode {

    public byte[] payload;
    public int offset;
    public int bitOffset;
    private static int[] CRC_TABLE = new int[256];
    private static final int[] BIT_MASKS;
    public ISAACCipher isaacCipher;
    private static int availableMode0Buffers;
    private static int availableMode1Buffers;
    private static int availableMode2Buffers;
    private static LinkedList mode0Pool;
    private static LinkedList mode1Pool;
    private static LinkedList mode2Pool;

    public static Buffer newPooledBuffer(int sizeMode) {
        synchronized (Buffer.mode1Pool) {
            Buffer buffer = null;
            if (sizeMode == 0 && Buffer.availableMode0Buffers > 0) {
                Buffer.availableMode0Buffers--;
                buffer = (Buffer) Buffer.mode0Pool.popTail();
            } else if (sizeMode == 1 && Buffer.availableMode1Buffers > 0) {
                Buffer.availableMode1Buffers--;
                buffer = (Buffer) Buffer.mode1Pool.popTail();
            } else if (sizeMode == 2 && Buffer.availableMode2Buffers > 0) {
                Buffer.availableMode2Buffers--;
                buffer = (Buffer) Buffer.mode2Pool.popTail();
            }
            if (buffer != null) {
                buffer.offset = 0;
                return buffer;
            }
        }
        Buffer buffer = new Buffer(null);
        buffer.offset = 0;
        if (sizeMode == 0) {
            buffer.payload = new byte[100];
        } else if (sizeMode == 1) {
            buffer.payload = new byte[5000];
        } else {
            buffer.payload = new byte[30000];
        }
        return buffer;
    }

    public Buffer(byte[] buffer) {
        this.payload = buffer;
        this.offset = 0;
    }

    public void putOpcode(int value) {
        payload[offset++] = (byte) (value + isaacCipher.nextInt());
    }

    public void put(int value) {
        payload[offset++] = (byte) value;
    }

    public void putShort(int value) {
        payload[offset++] = (byte) (value >> 8);
        payload[offset++] = (byte) value;
    }

    public void putLEShort(int value) {
        payload[offset++] = (byte) value;
        payload[offset++] = (byte) (value >> 8);
    }

    public void put24BitInt(int value) {
        payload[offset++] = (byte) (value >> 16);
        payload[offset++] = (byte) (value >> 8);
        payload[offset++] = (byte) value;
    }

    public void putInt(int value) {
        payload[offset++] = (byte) (value >> 24);
        payload[offset++] = (byte) (value >> 16);
        payload[offset++] = (byte) (value >> 8);
        payload[offset++] = (byte) value;
    }

    public void putLEInt(int value) {
        payload[offset++] = (byte) value;
        payload[offset++] = (byte) (value >> 8);
        payload[offset++] = (byte) (value >> 16);
        payload[offset++] = (byte) (value >> 24);
    }

    public void putLong(long value) {
        payload[offset++] = (byte) (int) (value >> 56);
        payload[offset++] = (byte) (int) (value >> 48);
        payload[offset++] = (byte) (int) (value >> 40);
        payload[offset++] = (byte) (int) (value >> 32);
        payload[offset++] = (byte) (int) (value >> 24);
        payload[offset++] = (byte) (int) (value >> 16);
        payload[offset++] = (byte) (int) (value >> 8);
        payload[offset++] = (byte) (int) value;
    }

    public void putString(String value) {
        System.arraycopy(value.getBytes(), 0, payload, offset, value.length());
        offset += value.length();
        payload[offset++] = (byte) 10;
    }

    public void putBytes(byte[] src, int length, int offset) {
        for (int i = offset; i < offset + length; i++) {
            payload[this.offset++] = src[i];
        }
    }

    public void putSizeByte(int value) {
        payload[offset - value - 1] = (byte) value;
    }

    public int getUnsignedByte() {
        return payload[offset++] & 0xff;
    }

    public byte get() {
        return payload[offset++];
    }

    public int getUnsignedShort() {
        offset += 2;
        return ((payload[offset - 1] & 0xff) << 8) + (payload[offset - 2] & 0xff);
    }

    public int getUnsignedLEShort() {
        offset += 2;
        return ((payload[offset - 2] & 0xff) << 8) + (payload[offset - 1] & 0xff);
    }

    public int getShort() {
        offset += 2;
        int i = ((payload[offset - 2] & 0xff) << 8) + (payload[offset - 1] & 0xff);
        if (i > 32767) {
            i -= 65536;
        }
        return i;
    }

    public int get24BitInt() {
        offset += 3;
        return ((payload[offset - 3] & 0xff) << 16) + ((payload[offset - 2] & 0xff) << 8)
                + (payload[offset - 1] & 0xff);
    }

    public int getInt() {
        offset += 4;
        return ((payload[offset - 4] & 0xff) << 24) + ((payload[offset - 3] & 0xff) << 16)
                + ((payload[offset - 2] & 0xff) << 8) + (payload[offset - 1] & 0xff);
    }

    public long getLong() {
        long l = getInt() & 0xffffffffL;
        long l_5_ = getInt() & 0xffffffffL;
        return (l << 32) + l_5_;
    }

    public String getString() {
        int originalOffset = offset;
        while (payload[offset++] != 10) {
            /* empty */
        }
        return new String(payload, originalOffset, offset - originalOffset - 1);
    }

    public byte[] getStringAsBytes() {
        int originalOffset = offset;
        while (payload[offset++] != 10) {
			/* empty */
        }
        byte[] value = new byte[offset - originalOffset - 1];
        for (int offset = originalOffset; offset < this.offset - 1; offset++) {
            value[offset - originalOffset] = payload[offset];
        }
        return value;
    }

    public void getBytes(byte[] src, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            src[i] = payload[this.offset++];
        }
    }

    public void initBitAccess() {
        bitOffset = offset * 8;
    }

    public int getBits(int size) {
        int byteOffset = bitOffset >> 3;
        int bitsToRead = 8 - (bitOffset & 0x7);
        int value = 0;
        bitOffset += size;
        for (; size > bitsToRead; bitsToRead = 8) {
            value += (payload[byteOffset++] & Buffer.BIT_MASKS[bitsToRead]) << size - bitsToRead;
            size -= bitsToRead;
        }
        if (size == bitsToRead) {
            value += payload[byteOffset] & Buffer.BIT_MASKS[bitsToRead];
        } else {
            value += payload[byteOffset] >> bitsToRead - size & Buffer.BIT_MASKS[size];
        }
        return value;
    }

    public void finishBitAccess() {
        offset = (bitOffset + 7) / 8;
    }

    public int getSmartA() {
        int value = payload[offset] & 0xff;
        if (value < 128) {
            return getUnsignedByte() - 64;
        }
        return getUnsignedLEShort() - 49152;
    }

    public int getSmartB() {
        int value = payload[offset] & 0xff;
        if (value < 128) {
            return getUnsignedByte();
        }
        return getUnsignedLEShort() - 32768;
    }

    public void applyRSA(BigInteger exponent, BigInteger modulus) {
        int originalOffset = offset;
        offset = 0;
        byte[] decodedBuffer = new byte[originalOffset];
        getBytes(decodedBuffer, 0, originalOffset);
        BigInteger decodedBigInteger = new BigInteger(decodedBuffer);
        BigInteger encodedBigInteger = null;
        // encodedBigInteger = decodedBigInteger.modPow(exponent, modulus);
        encodedBigInteger = decodedBigInteger;
        byte[] encodedBuffer = encodedBigInteger.toByteArray();
        offset = 0;
        put(encodedBuffer.length);
        putBytes(encodedBuffer, encodedBuffer.length, 0);
    }

    public void putByteC(int value) {
        payload[offset++] = (byte) -value;
    }

    public void putByteS(int value) {
        payload[offset++] = (byte) (128 - value);
    }

    public int getUnsignedByteA() {
        return payload[offset++] - 128 & 0xff;
    }

    public int getUnsignedByteC() {
        return -payload[offset++] & 0xff;
    }

    public int getUnsignedByteS() {
        return 128 - payload[offset++] & 0xff;
    }

    public byte getByteC() {
        return (byte) -payload[offset++];
    }

    public byte getByteS() {
        return (byte) (128 - payload[offset++]);
    }

    public void putShortA(int value) {
        payload[offset++] = (byte) (value >> 8);
        payload[offset++] = (byte) (value + 128);
    }

    public void putLEShortA(int value) {
        payload[offset++] = (byte) (value + 128);
        payload[offset++] = (byte) (value >> 8);
    }

    public int getUnsignedLEShortA() {
        offset += 2;
        return ((payload[offset - 2] & 0xff) << 8) + (payload[offset - 1] - 128 & 0xff);
    }

    public int getUnsignedShortA() {
        offset += 2;
        return ((payload[offset - 1] & 0xff) << 8) + (payload[offset - 2] - 128 & 0xff);
    }

    public int getForceLEShort() {
        offset += 2;
        int value = ((payload[offset - 1] & 0xff) << 8) + (payload[offset - 2] & 0xff);
        if (value > 32767) {
            value -= 65536;
        }
        return value;
    }

    public int getForceLEShortA() {
        offset += 2;
        int value = ((payload[offset - 1] & 0xff) << 8) + (payload[offset - 2] - 128 & 0xff);
        if (value > 32767) {
            value -= 65536;
        }
        return value;
    }

    public int getInt2() {
        offset += 4;
        return ((payload[offset - 2] & 0xff) << 24) + ((payload[offset - 1] & 0xff) << 16)
                + ((payload[offset - 4] & 0xff) << 8) + (payload[offset - 3] & 0xff);
    }

    public int getInt1() {
        offset += 4;
        return ((payload[offset - 3] & 0xff) << 24) + ((payload[offset - 4] & 0xff) << 16)
                + ((payload[offset - 1] & 0xff) << 8) + (payload[offset - 2] & 0xff);
    }

    public void putBytesA(int length, byte[] src, int offset) {
        for (int index = length + offset - 1; index >= length; index--) {
            payload[this.offset++] = (byte) (src[index] + 128);
        }
    }

    public void getBytes(int length, int offset, byte[] src) {
        for (int index = offset + length - 1; index >= offset; index--) {
            src[index] = payload[this.offset++];
        }
    }

    static {
        for (int index = 0; index < 256; index++) {
            int generatedCRC = index;
            for (int count = 0; count < 8; count++) {
                if ((generatedCRC & 0x1) == 1) {
                    generatedCRC = generatedCRC >>> 1 ^ ~0x12477cdf;
                } else {
                    generatedCRC >>>= 1;
                }
            }
            Buffer.CRC_TABLE[index] = generatedCRC;
        }
        BIT_MASKS = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535,
                131071, 262143, 524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727,
                268435455, 536870911, 1073741823, 2147483647, -1};
        Buffer.mode0Pool = new LinkedList();
        Buffer.mode1Pool = new LinkedList();
        Buffer.mode2Pool = new LinkedList();
    }
}
