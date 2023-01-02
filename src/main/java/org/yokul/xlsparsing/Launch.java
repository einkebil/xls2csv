package org.yokul.xlsparsing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.report.ResolveReport;

public class Launch {
	public static void main(String[] args) throws ClassNotFoundException, ParseException, IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Ivy ivy = Ivy.newInstance();
		URL confURL = Launch.class .getResource("ivy.xml");
		System.out.println(confURL);
		ivy.configureDefault();
		ResolveReport report = ivy.resolve(confURL);
		List<URL> urls = Arrays.asList(report.getAllArtifactsReports()).stream().map(artifact -> {
			try {
				return artifact.getLocalFile().toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(x -> x != null).collect(Collectors.toList());
		urls.add(new URL("file:xls-parsing-0.0.1.exe"));
		URL[] u = urls.toArray(new URL[urls.size()]);
		
		for (URL x:u) System.out.println(x);
		 ClassLoader cx = Launch.class.getClassLoader();
		 
		try (URLClassLoader cl = new URLClassLoader(u,null)) {
			System.out.println( cl .loadClass("org.apache.xmlbeans.impl.values.XmlComplexContentImpl").getCanonicalName());
			Class<?> clazz = cl.loadClass(first2log.class.getCanonicalName());
			
			Method main = clazz.getMethod("main", String[].class);
			Object[] arguments = new Object[] { args}; // the arguments. Change this if you want to pass different args
			main.invoke(cl, arguments);
		}
	}
}
