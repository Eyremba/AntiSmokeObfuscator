package me.grax.gezkm.injection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.HashMap;

public class InjectionClassLoader extends URLClassLoader {
	private final ClassLoader startup;
	public HashMap<String, byte[]> classes;

	public InjectionClassLoader(final ClassLoader startup, String... jar) throws MalformedURLException {
		super(getURLArr(jar));
		this.classes = new HashMap<String, byte[]>();
		this.startup = startup;
	}


	private static URL[] getURLArr(String[] jar) throws MalformedURLException {
		URL[] arr = new URL[jar.length];
		for (int i = 0; i < jar.length; i++) {
			arr[i] = new File(jar[i]).toURI().toURL();
		}
		return arr;
	}
	public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		try {
			final Class<?> aClass = this.startup.loadClass(name);
			if (aClass != null) {
				return aClass;
			}
		} catch (Exception ex) {
		}
		Class<?> clazz = this.findLoadedClass(name);
		if (clazz == null) {
			try {
				final InputStream in = this.getResourceAsStream(name.replace('.', '/') + ".class");
				if (in == null) {
					return null;
				}
				final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				copyStream(in, bytes);
				in.close();
				final byte[] ba = bytes.toByteArray();
				bytes.close();
				try {
					final Class<ClassLoader> clClass = ClassLoader.class;
					final Method m1 = clClass.getDeclaredMethod("preDefineClass", String.class, ProtectionDomain.class);
					m1.setAccessible(true);
					final Object protectionDomain = m1.invoke(this, name, null);
					final Method m2 = clClass.getDeclaredMethod("defineClassSourceLocation", ProtectionDomain.class);
					m2.setAccessible(true);
					final Object source = m2.invoke(this, protectionDomain);
					final Method m3 = clClass.getDeclaredMethod("defineClass1", String.class, byte[].class, Integer.TYPE, Integer.TYPE,
							ProtectionDomain.class, String.class);
					m3.setAccessible(true);
					final Object definedClass = m3.invoke(this, name, ba, 0, ba.length, protectionDomain, source);
					final Method m4 = clClass.getDeclaredMethod("postDefineClass", Class.class, ProtectionDomain.class);
					m4.setAccessible(true);
					m4.invoke(this, definedClass, protectionDomain);
					clazz = (Class<?>) definedClass;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (resolve) {
					this.resolveClass(clazz);
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				clazz = super.loadClass(name, resolve);
			}
		}
		return clazz;
	}

	@Override
	public URL getResource(final String name) {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(final String name) {
		final InputStream jarRes = this.startup.getResourceAsStream(name);
		if (jarRes != null) {
			return jarRes;
		}
		if (!this.classes.containsKey(name)) {
			return null;
		}
		return new ByteArrayInputStream(this.classes.get(name));
	}

	public static int copyStream(final InputStream in, final OutputStream out) throws IOException {
		int ttl = -1;
		final byte[] buffer = new byte[10024];
		for (int c = in.read(buffer); c > 0; c = in.read(buffer)) {
			out.write(buffer, 0, c);
			if (ttl < 0) {
				ttl = 0;
			}
			ttl += c;
		}
		return ttl;
	}
}
