package me.grax.gezkm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import me.grax.gezkm.injection.InjectionClassLoader;
import me.lpk.util.ASMUtils;
import me.lpk.util.JarUtils;

public class Unsmoke {

	public static InjectionClassLoader classLoader;
	private static ArrayList<String> libs = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		String input = "input.jar";
		File in = new File(input);
		if (in.exists()) {
			Map<String, ClassNode> classes = JarUtils.loadClasses(in);
			Map<String, byte[]> other = JarUtils.loadNonClassEntries(in);
			setupClassLoader(classes, other);
			try {
				Files.walk(new File("libraries").toPath()).filter(Files::isRegularFile).forEach(file -> {
					if (file.toString().endsWith(".jar")) {
						loadLib(file.toFile().getAbsolutePath());
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			Deobfuscator.startDeobfuscation(classes, other);
			for (ClassNode cn : classes.values()) {
				other.put(cn.name, ASMUtils.getNodeBytes(cn, true));
			}
			JarUtils.saveAsJar(other, "output.jar");
		} else {
			System.err.println("Input file can not be accessed!");
		}
	}

	private static void loadLib(String absolutePath) {
		libs.add(absolutePath);
	}

	public static byte[] streamToBytes(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[16384];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		buffer.flush();
		return buffer.toByteArray();
	}

	private static void setupClassLoader(Map<String, ClassNode> classes, Map<String, byte[]> other) throws MalformedURLException {
		classLoader = new InjectionClassLoader(Unsmoke.class.getClassLoader(), libs.toArray(new String[0]));
		Thread.currentThread().setContextClassLoader(classLoader);

		//load classes

		/* 
		 * we need to modify it first
		 * 
		classes.values().forEach(clazz -> {
			if (clazz.preLoad != null) {
				byte[] bytes = clazz.preLoad;
				classLoader.classes.put(clazz.name + ".class", bytes);
			} else {
				System.err.println("Bytes of class not found: " + clazz.name);
			}
		});
		
		*/

		other.keySet().forEach(file -> {
			classLoader.classes.put(file, other.get(file));
		});
	}
}
