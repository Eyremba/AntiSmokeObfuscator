package me.grax.gezkm.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import me.lpk.util.AccessHelper;

public class FieldChangeListener {

	private HashMap<String, Object> values = new HashMap<>();
	private Class clazz;

	public FieldChangeListener(Class c) {
		this.clazz = c;
		try {
			for (Field f : c.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					try {
						values.put(f.getName() + "." + f.getType().getName(), f.get(null));
					} catch (IllegalArgumentException | IllegalAccessException e) {
					}
				}
			}
		} catch (Throwable t) {
			System.err.println("class not found! " + c.getName());
		}
	}

	public ArrayList<Field> check() {
		try {
			ArrayList<Field> changed = new ArrayList<>();
			for (Field f : clazz.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					try {
						Object value = f.get(null);
						if (value != null)
							if (!value.equals(values.get(f.getName() + "." + f.getType().getName()))) {
								changed.add(f);
							}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
			return changed;
		} catch (Throwable t) {
			System.err.println("class not found! ");
			return new ArrayList<>();
		}
	}

}
