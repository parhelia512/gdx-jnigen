package com.badlogic.jnigen.benchmark;

import com.badlogic.gdx.jnigen.runtime.CHandler;
import com.badlogic.jnigen.generated.TestData;
import com.badlogic.jnigen.generated.enums.SpecialEnum;
import com.badlogic.jnigen.generated.enums.TestEnum;
import com.badlogic.jnigen.generated.structs.TestStruct;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

@State(Scope.Benchmark)
public class JNICallBenchmark {

    private TestStruct testStruct;
    private CharBuffer charBuffer = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).asCharBuffer();

    @Setup(Level.Trial)
    public void setup() {
        TestData.initialize();
        testStruct = new TestStruct();
    }

    @Benchmark
    public void allocationAndDeallocation(Blackhole bh) {
        long ptr = CHandler.calloc(1, 16);
        CHandler.free(ptr);
    }

    @Benchmark
    public void structFieldGet(Blackhole bh) {
        bh.consume(testStruct.field4());
    }

    @Benchmark
    public void structFieldSet(Blackhole bh) {
        testStruct.field4('a');
    }

    @Benchmark
    public void getEnumByIndexArray(Blackhole bh) {
        bh.consume(TestEnum.getByIndex(0));
    }

    @Benchmark
    public void getEnumByIndexMap(Blackhole bh) {
        bh.consume(SpecialEnum.getByIndex(160));
    }

    @Benchmark
    public void bufferFieldGet(Blackhole bh) {
        bh.consume(charBuffer.get(0));
    }

    @Benchmark
    public void bufferFieldSet(Blackhole bh) {
        charBuffer.put(0, 'a');
    }
}
