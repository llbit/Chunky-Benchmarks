/* Copyright (c) 2017, Jesper Öqvist
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package se.llbit.chunky;

import se.llbit.json.JsonObject;
import se.llbit.json.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool for benchmarking the Chunky renderer.
 */
public class Benchmark {
  public static void main(String[] args) throws IOException, InterruptedException {
    List<String> arguments = new ArrayList<>();
    Map<String, String> options = new HashMap<>();
    for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("--")) {
        if (i + 1 < args.length) {
          options.put(args[i], args[i + 1]);
          i += 1;
        } else {
          options.put(args[i], "");
        }
      } else {
        arguments.add(args[i]);
      }
    }
    if (arguments.isEmpty()) {
      System.err.println("No mode selected.");
      printUsage();
      System.exit(1);
      return;
    }
    switch (arguments.get(0)) {
      case "benchmark":
      case "bench":
        runBenchmark(arguments, options);
        break;
      case "statistics":
      case "stats":
        collectStats();
        break;
      default:
        System.err.format("Unknown mode: %s%n", arguments.get(0));
        printUsage();
        System.exit(1);
        return;
    }
  }

  private static void printUsage() {
    System.out.println("Usage: benchmark.jar <MODE> [OPTIONS]");
    System.out.println("Where MODE is one of:");
    System.out.println("    bench <chunky-jar> [--target SPP]");
    System.out.println("    stats        - collect statistics from benchmark run");
  }

  private static void collectStats() {
    File temp = new File("temp");
    File tempScenes = new File(temp, "scenes");
    tempScenes.mkdir();
    System.out.println("scene time spp chunks emitters width height");
    forEachFile(tempScenes, file -> {
      withScene(file, name -> {
        try {
          JsonParser parser = new JsonParser(new FileInputStream(file));
          JsonObject scene = parser.parse().object();
          int time = scene.get("renderTime").intValue(0);
          int spp = scene.get("spp").intValue(0);
          int numChunks = scene.get("chunkList").array().getNumElement();
          boolean emitters = scene.get("emittersEnabled").boolValue(false);
          int width = scene.get("width").intValue(0);
          int height = scene.get("height").intValue(0);
          System.out.format("%s %d %d %d %s %d %d%n", name, time, spp, numChunks,
              emitters ? "TRUE" : "FALSE", width, height);
        } catch (IOException | JsonParser.SyntaxError e) {
          throw new Error(e);
        }
      });
    });
  }

  private static void runBenchmark(List<String> arguments, Map<String, String> options)
      throws IOException, InterruptedException {
    if (arguments.size() < 2) {
      System.err.println("Missing chunky.jar argument.");
      printUsage();
      System.exit(1);
      return;
    }
    String chunkyJar = arguments.get(1);
    int target = 1000;  // Target samples per pixel.
    if (options.containsKey("--target")) {
      target = Integer.parseInt(options.get("--target"));
    }
    File chunky = new File(chunkyJar);
    if (!chunky.isFile()) {
      System.err.format("Error: %s does not seem name a Chunky Jar file!%n", chunkyJar);
      printUsage();
      System.exit(1);
      return;
    }
    File sceneDir = new File("scenes");
    if (!sceneDir.isDirectory()) {
      System.err.println("Error: missing scene directory!");
      System.exit(1);
      return;
    }
    File temp = new File("temp");
    if (temp.isDirectory()) {
      cleanDirectory(temp);
    } else {
      temp.mkdir();
    }
    File tempScenes = new File(temp, "scenes");
    tempScenes.mkdir();
    copyScenes(sceneDir, tempScenes);
    setup(chunky, temp, tempScenes);
    final int targetSpp = target;
    forEachFile(sceneDir, file -> {
      withScene(file, name -> {
        try {
          benchmark(chunky, temp, tempScenes, name, targetSpp);
        } catch (IOException | InterruptedException e) {
          throw new Error(e);
        }
      });
    });
  }

  private static void forEachFile(File dir, Consumer<File> consumer) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        consumer.accept(file);
      }
    }
  }

  private static void cleanDirectory(File temp) {
    forEachFile(temp, file -> {
      if (file.isDirectory()) {
        cleanDirectory(file);
      } else {
        file.delete();
      }
    });
  }

  private static void copyScenes(File sceneDir, File temp) {
    forEachFile(sceneDir, file -> {
      withScene(file, name -> {
        try {
          copyFile(sceneDir, temp, name + ".json");
          copyFile(sceneDir, temp, name + ".octree");
          copyFile(sceneDir, temp, name + ".grass");
          copyFile(sceneDir, temp, name + ".foliage");
        } catch (IOException e) {
          throw new Error(e);
        }
      });
    });
  }

  private static void copyFile(File srcDir, File destDir, String name) throws IOException {
    File source = new File(srcDir, name);
    File target = new File(destDir, name);
    Files.copy(source.toPath(), target.toPath());
  }

  private static void withScene(File file, Consumer<String> consumer) {
    if (file.getName().endsWith(".json")) {
      String name = file.getName();
      consumer.accept(name.substring(0, name.length() - 5));
    }
  }

  /** Set up benchmark environment and create Chunky configuration. */
  private static void setup(File chunky, File temp, File tempScenes)
      throws IOException, InterruptedException {
    // Configure memory limit.
    PrintStream settings = new PrintStream(
        new FileOutputStream(new File(temp, "chunky-launcher.json")));
    settings.println("{");
    settings.println("  \"memoryLimit\": 4096");
    settings.println("}");

    // Download Minecraft (default textures).
    ProcessBuilder pb = new ProcessBuilder("java",
        "-Dchunky.home=" + temp.getAbsolutePath(),
        "-jar", chunky.getAbsolutePath(),
        "-download-mc", "1.11");
    pb.inheritIO();
    Process process = pb.start();
    int result = process.waitFor();
    if (result != 0) {
      throw new Error("Failed to setup benchmark environment.");
    }
  }

  /**
   * @param targetSpp the target SPP for this benchmark run.
   */
  private static void benchmark(File chunky, File temp, File tempScenes, String name,
      int targetSpp) throws IOException, InterruptedException {
    System.out.format("Starting benchmark: %s%n", name);
    ProcessBuilder pb = new ProcessBuilder("java",
        "-Dchunky.home=" + temp.getAbsolutePath(),
        "-jar", chunky.getAbsolutePath(),
        "-scene-dir", tempScenes.getAbsolutePath(),
        "-render", name,
        "-target", String.valueOf(targetSpp));
    pb.inheritIO();
    Process process = pb.start();
    int result = process.waitFor();
    if (result != 0) {
      System.err.format("Benchmark %s failed with exit code %d.%n", name, result);
    }
  }
}
