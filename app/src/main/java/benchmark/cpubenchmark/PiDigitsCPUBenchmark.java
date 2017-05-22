package benchmark.cpubenchmark;

import android.util.Log;
import benchmark.IBenchmark;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Vendetta on 09-Mar-17.
 */

/**
 * stresses the CPU by computing digits of PI, in a parallel manner.
 * The algorithms is based on the Bailey–Borwein–Plouffe formula.
 * The sum is split into 8 chunks, each being processed by a separate thread.
 */
public class PiDigitsCPUBenchmark implements IBenchmark {
    private static final int THREAD_POOL_SIZE = 4;     // Number of threads.
    private static final int TOTAL_ITERATIONS = 10000; // More iterations results in better accuracy.

    private boolean shouldTestRun;
    private BigDecimal piResult = new BigDecimal(0);
    private int scale = 1000; // The scale should usually be equal to TOTAL_ITERATIONS

    @Override
    public void initialize() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param size How many digits to compute.
     */
    @Override
    public void initialize(Long size) {
        this.scale = size.intValue();
    }

    public void warmup(){
        int prevScale = this.scale;
        this.scale = 100;
        this.run();
        this.scale = prevScale;
    }

    @Override
    public void run(Object... param) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void run() {
        this.shouldTestRun = true;
        MathContext context = new MathContext(this.scale);
        final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        final int chunkSize = TOTAL_ITERATIONS / THREAD_POOL_SIZE;
        ArrayList<Future<BigDecimal>> chunksResult = new ArrayList<>();

        for (int chunkStart = 0; chunkStart < TOTAL_ITERATIONS; chunkStart += chunkSize) {
            chunksResult.add(threadPool.submit(new ComputeChunk(chunkStart, Math.min(chunkStart + chunkSize, TOTAL_ITERATIONS), context)));
        }
        for (Future<BigDecimal> result : chunksResult) {
            if (!this.shouldTestRun) {
                break;
            }
            try {
                piResult = piResult.add(result.get(), context);
            } catch (Exception e) {
                if (shouldTestRun) {
                    // Something went wrong
                    //TODO What do we do now ?
                    Log.d(PiDigitsCPUBenchmark.class.getName(), e.getMessage());
                }
            }
        }
        threadPool.shutdownNow();
        this.shouldTestRun = false;
    }

    @Override
    public void stop() {
        this.shouldTestRun = false;
    }

    @Override
    public void clean() {}

    public BigDecimal getPi(){
        return this.piResult;
    }

    /**
     * Does part of the PI computation.
     */
    private class ComputeChunk implements Callable<BigDecimal> {
        private int begin;
        private int end;
        private MathContext context;

        ComputeChunk(int begin, int end, MathContext context) {
            this.begin = begin;
            this.end = end;
            this.context = context;
        }

        @Override
        public BigDecimal call() {
            final BigDecimal powerBase = new BigDecimal(1.0 / 16);
            BigDecimal sigma = new BigDecimal(0);
            BigDecimal pwr = powerBase.pow(begin, context);
            BigDecimal sum;
            final BigDecimal[] terms = new BigDecimal[4];
            terms[0] = new BigDecimal(4);
            terms[1] = new BigDecimal(2);
            terms[2] = new BigDecimal(1);
            terms[3] = new BigDecimal(1);

            for (int i = begin; i < end && shouldTestRun; i++) {
                sum = terms[0].divide(BigDecimal.valueOf(8 * i + 1), context);
                sum = sum.subtract(terms[1].divide(BigDecimal.valueOf(8 * i + 4), context), context);
                sum = sum.subtract(terms[2].divide(BigDecimal.valueOf(8 * i + 5), context), context);
                sum = sum.subtract(terms[3].divide(BigDecimal.valueOf(8 * i + 6), context), context);
                sigma = sigma.add(pwr.multiply(sum, context), context);
                pwr = pwr.multiply(powerBase, context);
            }
            return sigma;
        }
    }


    @Override
    public String getInfo(){
        return "";
    }
}