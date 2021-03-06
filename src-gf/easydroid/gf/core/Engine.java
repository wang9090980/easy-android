/*
 * Copyright 2012 Evgeny Dolganov (evgenij.dolganov@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package easydroid.gf.core;

import java.util.Collection;
import java.util.Properties;


import easydroid.core.annotation.Inject;
import easydroid.core.exception.ExceptionWrapper;
import easydroid.gf.Action;
import easydroid.gf.Filter;
import easydroid.gf.Handler;
import easydroid.gf.Interceptor;
import easydroid.gf.config.ConfigKey;
import easydroid.gf.core.action.ActionServiceImpl;
import easydroid.gf.core.config.ConfigServiceImpl;
import easydroid.gf.core.context.ContextServiceImpl;
import easydroid.gf.core.context.StaticContext;
import easydroid.gf.core.deploy.DeployServiceImpl;
import easydroid.gf.core.deploy.ResourseService;
import easydroid.gf.exception.config.EmptyClassException;
import easydroid.gf.exception.config.GetConfigValueException;
import easydroid.gf.exception.config.ParsePropertiesException;
import easydroid.gf.exception.deploy.NoMappingAnnotationException;
import easydroid.gf.exception.deploy.NotOneHandlerException;
import easydroid.gf.exception.invoke.InvocationException;
import easydroid.gf.extra.invocation.InvocationObjectInitializer;
import easydroid.gf.extra.scan.ClassScanner;
import easydroid.gf.key.scan.ClassScannerKey;
import easydroid.gf.service.ActionService;
import easydroid.gf.service.ConfigService;
import easydroid.gf.service.ContextService;
import easydroid.gf.service.DeployService;

/**
 * <tt>Engine</tt> is the central class of <tt>"Green-forest Framework"</tt>.
 * <br>Use it for create object with Action-Handler API:
 * <p>
 * <pre>
 * Engine engine = new Engine();
 * engine.putHandler(SomeActonHandler.class);
 * String result = engine.invoke(new SomeAction("some data"));
 * 
 * SomeAction otherAction = new SomeAction("other data");
 * engine.invoke(otherAction);
 * String otherResult = otherAction.getOutput();
 * </pre>
 * <p>
 * 
 * @author Evgeny Dolganov
 * @see Action
 * @see Handler
 * @see Interceptor
 * @see Filter
 *
 */
public class Engine implements ActionService, DeployService, ConfigService, ContextService {
	
	private String name;
	private ConfigService config;
	private DeployService deploy;
	private ActionService actions;
	private ContextService context;
	
	/**
	 * Create new instance of <tt>Engine</tt> with empty name.
	 * <p>Use engine's <tt>name</tt> for any information reason (for logging for example).
	 */
	public Engine() {
		this(null);
	}
	
	/**
	 * Create new instance of <tt>Engine</tt> with <tt>name</tt>
	 * <p>Use engine's <tt>name</tt> for any information reason (for logging for example).
	 */
	public Engine(String name) {

		this.name = name;
		config = new ConfigServiceImpl();
		deploy = new DeployServiceImpl(config);
		context = new ContextServiceImpl();
		actions = new ActionServiceImpl(
				this,
				config, 
				(ResourseService)deploy,
				(StaticContext)context);
		
	}

	/**
	 * Get current name of this <tt>Engine</tt>.
	 * <p>Use engine's <tt>name</tt> for any information reason (for logging for example).
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set new name for this <tt>Engine</tt>.
	 * <p>Use engine's <tt>name</tt> for any information reason (for logging for example).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Handle the {@link Action} object with this <tt>Engine</tt>.
	 * <br>Use {@link Handler}, {@link Interceptor}, {@link Filter} for this.
	 * <p>The order of processing:
	 * <ul>
	 * <li>Filters</li>
	 * <li>Interceptors</li>
	 * <li>Handler</li>
	 * </ul>
	 * <p>Interceptors and Handlers can call <tt>subInvoke</tt> method.
	 * <br>In this case the order of processing:
	 * <ul>
	 * <li>Filters <b>(was called only once)</b></li>
	 * <li>Interceptors
	 * 		<ul>
	 * 			<li>Sub Interceptors</li>
	 * 			<li>Sub Handler</li>
	 * 		</ul>
	 * </li>
	 * <li>Handler
	 * 		<ul>
	 * 			<li>Sub Interceptors</li>
	 * 			<li>Sub Handler</li>
	 * 		</ul>
	 * </li>
	 * </ul>
	 * @return <tt>Action</tt> output object.
	 * <br><b>Note:</b> This return value is the equivalent of code {@code action.getOutput()} after the invoke method.
	 * @throws InvocationException <tt>Engine</tt>'s processing exception
	 * @throws ExceptionWrapper wrapper of non-runtime <tt>Exception</tt> from handler's body
	 * @throws RuntimeException runtime <tt>Exception</tt> from handler's body
	 * @see #invokeUnwrap(Action)
	 * @see Action
	 * @see Handler
	 * @see Interceptor
	 * @see Filter
	 */
	@Override
	public <I, O> O invoke(Action<I, O> action) 
			throws InvocationException, ExceptionWrapper, RuntimeException {
		return (O)actions.invoke(action);
	}
	
	/**
	 * This method is {@link Engine#invoke(Action)} analog with unwrap non-runtime exceptions.
	 * <p>Example:
	 * <pre>
	 * &#064;Mapping(SomeAction.class)
	 * public class ActionHandlerWithException extends Handler&lt;SomeAction&gt;{
	 * 
	 *   public void invoke(SomeAction action) throws Exception {
	 *     //not-runtime exception
	 *     throw new Exception("test expection");
	 *   }
	 *   
	 * }
	 * 
	 * ...
	 * 
	 * //use invokeUnwrap
	 * try {
	 *   engine.invokeUnwrap(new SomeAction());
	 * }catch (Exception e) {
	 *   System.out.println(e);
	 * }
	 * 
	 * //use invoke
	 * engine.invoke(new SomeAction()); //throws ExceptionWrapper
	 * </pre>
	 * 
	 * @see #invoke(Action)
	 */
	@Override
	public <I, O> O invokeUnwrap(Action<I, O> action) 
			throws InvocationException, Exception {
		return (O)actions.invokeUnwrap(action);
	}
	
	/**
	 * Put the <tt>Handler</tt> class into this <tt>Engine</tt>.
	 */
	@Override
	public void putHandler(Class<? extends Handler<?>> clazz)
			throws NoMappingAnnotationException, NotOneHandlerException {
		deploy.putHandler(clazz);
	}

	/**
	 * Put the <tt>Interceptor</tt> class into this <tt>Engine</tt>.
	 */
	@Override
	public void putInterceptor(Class<? extends Interceptor<?>> clazz)
			throws NoMappingAnnotationException {
		deploy.putInterceptor(clazz);
	}

	/**
	 * Put the <tt>Filter</tt> class into this <tt>Engine</tt>.
	 */
	@Override
	public void putFilter(Class<? extends Filter> clazz) {
		deploy.putFilter(clazz);
	}
	
	/**
	 * Scan packages and put all handlers, interceptors, filters into this <tt>Engine</tt>.
	 * <p>Example:
	 * <pre>
	 * //handler
	 * package some.package.subPackage;
	 * &#064;Mapping(SomeAction.class)
	 * public class SomeHandler extends Handler&lt;SomeAction&gt;{...}
	 * 
	 * //engine
	 * Engine engine = new Engine();
	 * engine.scanAndPut("some.package");
	 * engine.invoke(new SomeAction());
	 * </pre>
	 * 
	 * <p><b>Note:</b> For some Application Servers (JBoss AS for example)
	 * you need to setup {@link com.gf.components.reflections.ReflectionsScanner}.
	 * Or you can use your own {@link ClassScanner}.
	 * <p>
	 * @param packageName root package for scan (example: "com.some.package")
	 * @see ClassScanner
	 * @see ClassScannerKey
	 */
	@Override
	public void scanAndPut(String packageName)
			throws NoMappingAnnotationException, NotOneHandlerException {
		deploy.scanAndPut(packageName);
	}
	
	/**
	 * Analog for {@link #scanAndPut(String)}: <tt>scanAndPut(clazz.getPackage().getName())</tt>
	 * 
	 * <p><b>Note:</b> For some Application Servers (JBoss AS for example)
	 * you need to setup {@link com.gf.components.reflections.ReflectionsScanner}.
	 * Or you can use your own {@link ClassScanner}.
	 * <p>
	 */
	@Override
	public void scanAndPut(Class<?> clazz)
			throws NoMappingAnnotationException, NotOneHandlerException {
		deploy.scanAndPut(clazz);
	}
	
	
	/**
	 * Analog of multi call of {@link #scanAndPut(String)}.
	 * For example for JavaBean logic.
	 * 
	 * <p><b>Note:</b> For some Application Servers (JBoss AS for example)
	 * you need to setup {@link com.gf.components.reflections.ReflectionsScanner}.
	 * Or you can use your own {@link ClassScanner}.
	 * <p>
	 */
	@Override
	public void setScanAndPut(Collection<String> packageNames)
			throws NoMappingAnnotationException, NotOneHandlerException {
		deploy.setScanAndPut(packageNames);
	}
	
	
	/**
	 * Set config value by key type. You can create own config keys for using in handlers.
	 * <br>Example of using:
	 * <pre>
	 * //key
	 * public class SomeConfigKey extends ConfigKey&lt;String&gt;{
	 * 
	 *   public boolean hasDefaultValue() {
	 *     return true;
	 *   }
	 *   
	 *   public String getDefaultValue() throws Exception {
	 *     return "some default value";
	 *   }
	 * }
	 * 
	 * //handler
	 * &#064;Mapping(SomeAction.class)
	 * public class SomeHandler extends Handler&lt;SomeAction&gt;{
	 * 
	 *   public void invoke(SomeAction action) throws Exception {
	 *     String value = config.getConfig(new SomeConfigKey());
	 *     log.info(value);
	 *   }
	 * }
	 * 
	 * //engine
	 * Engine engine = new Engine();
	 * engine.putHandler(SomeHandler.class);
	 * engine.setConfig(SomeConfigKey.class, "some value");
	 * engine.invoke(new SomeAction());
	 * </pre>
	 * @param keyType not null type
	 * @param value some value for this config type (can be null)
	 * @throws EmptyClassException if <tt>keyType</tt> is null
	 * @see ConfigKey
	 */
	@Override
	public <T> void setConfig(Class<? extends ConfigKey<T>> keyType, T value) throws EmptyClassException {
		config.setConfig(keyType, value);
	}
	
	/**
	 * Get config value or default value or exception if key doesn't have default value.
	 * <br>Example:
	 * <pre>
	 * //key with default value
	 * public class SomeConfigKey extends ConfigKey&lt;String&gt;{
	 * 
	 *   public boolean hasDefaultValue() {
	 *     return true;
	 *   }
	 *   
	 *   public String getDefaultValue() throws Exception {
	 *     return "some default value";
	 *   }
	 * }
	 * 
	 * //key without default value
	 * public class OtherConfigKey extends ConfigKey&lt;String&gt;{
	 * 
	 *   public boolean hasDefaultValue() {
	 *     return false;
	 *   }
	 * }
	 * 
	 * //engine
	 * Engine engine = new Engine();
	 * 
	 * String val1 = engine.getConfig(new SomeConfigKey());
	 * System.out.println(val1); //<--------- print "some default value"
	 *     
	 * engine.setConfig(SomeConfigKey.class, "some value");
	 * String val2 = engine.getConfig(new SomeConfigKey());
	 * System.out.println(val2); //<--------- print "some value"
	 *     
	 * engine.getConfig(new OtherConfigKey()); //<---------- get GetConfigValueException
	 * </pre>
	 * 
	 * @see ConfigKey
	 */
	@Override
	public <T> T getConfig(ConfigKey<T> key) throws GetConfigValueException {
		return (T) config.getConfig(key);
	}

	/**
	 * Get boolean value of <tt>Boolean</tt> config key. If value is <tt>null</tt> return false.
	 * Anolog of {@link #getConfig(ConfigKey)}: <tt>Boolean.TRUE.equals(getConfig(key))</tt>
	 * 
	 * @see ConfigKey
	 */
	@Override
	public boolean isTrueConfig(ConfigKey<Boolean> key) throws GetConfigValueException {
		return config.isTrueConfig(key);
	}
	
	/**
	 * Parse config values from Properties. Key must be a config key's <tt>Class</tt> string.
	 * <br>For example:
	 * <pre>
	 * //file config.properties
	 * some.package.SomeConfigKey=value 1
	 * some.package.OtherConfigKey=value 2
	 * com.gf.key.TraceHandlers=true 
	 * 
	 * //props
	 * InputStream is = getResourceAsStream("config.properties");
	 * Properties props = new Properties();
	 * props.load(is);
	 * 
	 * //engine
	 * Engine engine = new Engine();
	 * engine.setConfigValues(props);
	 * String val1 = engine.getConfig(new SomeConfigKey());
	 * String val2 = engine.getConfig(new OtherConfigKey());
	 * boolean val3 = engine.isTrueConfig(new TraceHandlers());
	 * System.out.println(val1); //print "value 1"
	 * System.out.println(val2); //print "value 2"
	 * System.out.println(val3); //print "true"
	 * </pre>
	 * 
	 * @see ConfigKey
	 */
	@Override
	public void setConfigValues(Properties props) throws ParsePropertiesException {
		config.setConfigValues(props);
	}
	
	/**
	 * Add some object to current engine context.
	 * So this object can be injected into filters, interceptors, handlers.
	 * <p>Example:
	 * <pre>
	 * //engine
	 * SomeService someService = new SomeService();
	 * OtherService otherService = new OtherService();
	 * Engine engine = new Engine();
	 * engine.addToContext(someService);
	 * engine.addToContext(otherService);
	 * engine.putHandler(SomeHandler.class);
	 * engine.invoke(new SomeAction());
	 * 
	 * //handler
	 * &#064;Mapping(SomeAction.class)
	 * public class SomeHandler extends Handler&lt;SomeAction&gt;{
	 *   
	 *   &#064;Inject
	 *   SomeService service;
	 *   
	 *   &#064;Inject
	 *   OtherService otherService;
	 *   
	 *   &#064;Override
	 *   public void invoke(SomeAction action) throws Exception {
	 *   
	 *     service.someMethod();
	 *     otherService.invoke(new OtherAction()); //it can be an another Engine. Why not?
	 *     
	 *   }
	 * }
	 * </pre>
	 * 
	 * @see Inject
	 */
	@Override
	public void addToContext(Object object) {
		context.addToContext(object);
	}
	
	/**
	 * Analog of multi call of {@link #addToContext(Object)}.
	 * For example for JavaBean logic.
	 */
	@Override
	public void setContextObjects(Collection<Object> objects) {
		context.setContextObjects(objects);
	}

	/**
	 * Analog of multi call of {@link #putHandler(Class)}.
	 * For example for JavaBean logic.
	 */
	@Override
	public void setHandlerTypes(Collection<Class<? extends Handler<?>>> handlerTypes)
			throws NoMappingAnnotationException, NotOneHandlerException {
		deploy.setHandlerTypes(handlerTypes);
	}

	/**
	 * Analog of multi call of {@link #putInterceptor(Class)}.
	 * For example for JavaBean logic.
	 */
	@Override
	public void setInterceptorTypes(Collection<Class<? extends Interceptor<?>>> interceptorTypes)
			throws NoMappingAnnotationException{
		deploy.setInterceptorTypes(interceptorTypes);
	}

	/**
	 * Analog of multi call of {@link #putFilter(Class)}.
	 * For example for JavaBean logic.
	 */
	@Override
	public void setFilterTypes(Collection<Class<? extends Filter>> filterTypes) {
		deploy.setFilterTypes(filterTypes);
	}

	/**
	 * Put the <tt>InvocationObjectInitializer</tt> class into this <tt>Engine</tt>.
	 */
	@Override
	public void putInitializer(InvocationObjectInitializer initializer) {
		deploy.putInitializer(initializer);
	}

	/**
	 * Analog of multi call of {@link #putInitializer(InvocationObjectInitializer)}.
	 * For example for JavaBean logic.
	 */
	@Override
	public void setInitializers(Collection<InvocationObjectInitializer> initializers) {
		deploy.setInitializers(initializers);
	}

	

}
