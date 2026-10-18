package com.creativeblisslabs.omnimind.chess.engine

class StockfishNative {
    init {
        System.loadLibrary("stockfish_jni")
    }
    external fun initNative(storagePath: String): Long
    external fun sendNativeCommand(handle: Long, command: String)
    external fun readNativeOutput(handle: Long): String?
    external fun destroyNative(handle: Long)
}