package me.megamichiel.animationlib.util.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class IntPipeline {

    private final Runnable closer;
    private final List<IntPredicate> values = new ArrayList<>();

    IntPipeline(Runnable closer) {
        this.closer = closer;
    }

    public void accept(int i) {
        for (Iterator<IntPredicate> it = values.iterator(); it.hasNext();)
            if (it.next().test(i))
                it.remove();
    }

    public IntPipeline filter(IntPredicate predicate) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(i -> {
            if (predicate.test(i)) pipeline.accept(i);
        });
        return pipeline;
    }

    public IntPipeline exclude(IntPredicate predicate) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(i -> {
            if (!predicate.test(i)) pipeline.accept(i);
        });
        return pipeline;
    }
    
    public IntPipeline map(IntUnaryOperator mapper) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(i -> pipeline.accept(mapper.applyAsInt(i)));
        return pipeline;
    }
    
    public <U> Pipeline<U> mapToObj(IntFunction<? extends U> mapper) {
        Pipeline<U> pipeline = new Pipeline<>(closer);
        forEach(i -> pipeline.accept(mapper.apply(i)));
        return pipeline;
    }
    
    public LongPipeline mapToLong(IntToLongFunction mapper) {
        LongPipeline pipeline = new LongPipeline(closer);
        forEach(i -> pipeline.accept(mapper.applyAsLong(i)));
        return pipeline;
    }
    
    public DoublePipeline mapToDouble(IntToDoubleFunction mapper) {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(i -> pipeline.accept(mapper.applyAsDouble(i)));
        return pipeline;
    }
    
    public IntPipeline flatMap(IntFunction<? extends IntPipeline> mapper) {
        IntPipeline pipeline = new IntPipeline(closer);
        forEach(i -> mapper.apply(i).forEach(pipeline::accept));
        return pipeline;
    }

    public IntPipeline acceptWhile(BooleanSupplier supplier) {
        IntPipeline pipeline = new IntPipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                pipeline.accept(e);
                return false;
            }
            return true;
        });
        return pipeline;
    }

    public IntPipeline acceptUntil(BooleanSupplier supplier) {
        IntPipeline pipeline = new IntPipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) return true;
            pipeline.accept(e);
            return false;
        });
        return pipeline;
    }

    public IntPipeline acceptWhileBefore(long time) {
        return acceptWhile(() -> System.currentTimeMillis() < time);
    }

    public IntPipeline acceptUntil(long time) {
        return acceptUntil(() -> System.currentTimeMillis() >= time);
    }

    public IntPipeline skipUntil(BooleanSupplier supplier) {
        IntPipeline pipeline = new IntPipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) {
                forEach(pipeline::accept);
                return true;
            }
            return false;
        });
        return pipeline;
    }

    public IntPipeline skipUntil(long time) {
        return skipUntil(() -> System.currentTimeMillis() >= time);
    }

    public IntPipeline skipWhile(BooleanSupplier supplier) {
        IntPipeline pipeline = new IntPipeline(closer);
        values.add(e -> {
            if (supplier.getAsBoolean()) return false;
            forEach(pipeline::accept);
            return true;
        });
        return pipeline;
    }

    public IntPipeline limit(long maxSize) {
        AtomicLong l = new AtomicLong(maxSize);
        return acceptUntil(() -> l.decrementAndGet() < 0);
    }

    public IntPipeline skip(long n) {
        AtomicLong l = new AtomicLong(n);
        return skipUntil(() -> l.decrementAndGet() < 0);
    }

    public IntPipeline peek(IntConsumer action) {
        forEach(action);
        return this;
    }
    
    public void forEach(IntConsumer action) {
        values.add(i -> {
            action.accept(i);
            return false;
        });
    }
    
    public LongPipeline asLongPipeline() {
        LongPipeline pipeline = new LongPipeline(closer);
        forEach(pipeline::accept);
        return pipeline;
    }
    
    public DoublePipeline asDoublePipeline() {
        DoublePipeline pipeline = new DoublePipeline(closer);
        forEach(pipeline::accept);
        return pipeline;
    }
    
    public Pipeline<Integer> boxed() {
        return mapToObj(Integer::new);
    }

    public void unregister() {
        closer.run();
    }
}