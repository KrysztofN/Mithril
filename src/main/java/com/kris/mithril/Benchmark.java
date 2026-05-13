package com.kris.mithril;

import com.kris.mithrilAST.Interpreter;
import com.kris.mithrilVM.Chunk;
import com.kris.mithrilVM.Compiler;
import com.kris.mithrilVM.VM;
import org.jfree.ui.RefineryUtilities;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Benchmark {
    private static final PrintStream NULL_OUT = new PrintStream(new OutputStream() { public void write(int b) {} });
    private static final PrintStream REAL_OUT = System.out;
    static HashMap<String, Long[]> times = new HashMap<>();

    public static void main(String[] args) throws IOException {
        for (String script : new String[]{"bench1.mithril", "bench2.mithril", "bench3.mithril"}) {
            System.out.println("=== " + script + " ===");

            String path = "src/main/resources/" + script;
            String source = new String(Files.readAllBytes(Paths.get(path)), Charset.defaultCharset());
//            System.out.println(source);
            List<Token> tokens = new Scanner(source).scanTokens();
            List<Stmt> statements = new Parser(tokens).parseProgram();
            Chunk chunk = new Compiler().compile(statements);

            long tree = time(() -> new Interpreter().interpret(statements));
            long vm = time(() -> new VM(chunk).run());

            times.put(script.split("\\.")[0], new Long[]{tree, vm});

            System.out.println("Tree-walk : " + fmt(tree));
            System.out.println("VM        : " + fmt(vm));
            System.out.printf("Speedup   : %.2fx%n", (double) tree / vm);
        }

        BarChart chart = new BarChart("chart", "interpreter vs vm speed comparison", times);
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
        chart.save("barChart.jpeg");
    }

    private static long time(Runnable task) {
        for (int i = 0; i < 2; i++) run(task);
        long total = 0;
        for (int i = 0; i < 3; i++) total += run(task);
        return total / 3;
    }

    private static long run(Runnable task) {
        Mithril.hadError = false;
        Mithril.hadRuntimeError = false;

        System.setOut(NULL_OUT);
        long start = System.nanoTime();
        task.run();
        long end = System.nanoTime();
        System.setOut(REAL_OUT);
        return end - start;
    }

    private static String fmt(long nanos) {
        if (nanos < 1_000_000) return nanos / 1_000 + " µs";
        if (nanos < 1_000_000_000) return nanos / 1_000_000 + " ms";
        return String.format("%.2f s", nanos / 1_000_000_000.0);
    }
}