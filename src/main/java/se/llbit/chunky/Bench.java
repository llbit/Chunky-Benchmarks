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

public class Bench {
  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length > 0 && args[0].equals("-stats")) {
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
    } else if (args.length > 1 && args[0].equals("-bench")) {
      File chunky = new File(args[1]);
      if (!chunky.isFile()) {
        System.err.format("Error: %s does not seem name a Chunky Jar file!%n", args[0]);
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
      forEachFile(sceneDir, file -> {
        withScene(file, name -> {
          try {
            bench(chunky, temp, tempScenes, name);
          } catch (IOException | InterruptedException e) {
            throw new Error(e);
          }
        });
      });
    } else {
      System.err.println("Usage: -stats | -bench <chunky-jar>");
      System.exit(1);
    }
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

  private static void bench(File chunky, File temp, File tempScenes, String name)
      throws IOException, InterruptedException {
    System.out.format("Starting benchmark: %s%n", name);
    ProcessBuilder pb = new ProcessBuilder("java",
        "-Dchunky.home=" + temp.getAbsolutePath(),
        "-jar", chunky.getAbsolutePath(),
        "-scene-dir", tempScenes.getAbsolutePath(), "-render", name);
    pb.inheritIO();
    Process process = pb.start();
    int result = process.waitFor();
    if (result != 0) {
      System.err.format("Benchmark %s failed with exit code %d.%n", name, result);
    }
  }
}
