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
package easydroid.gf;

import easydroid.gf.annotation.Mapping;
import easydroid.gf.annotation.Order;
import easydroid.gf.service.InterceptorChain;

/**
 * Use <tt>Interceptor</tt> for handle some <tt>Action</tt> before targeted <tt>Handler</tt>.
 * 
 * <p><b>Note:</b> 
 * <ul>
 *   <li><tt>Interceptor</tt> must contains <tt>&#064;Mapping</tt> annotation to be valid for <tt>Engine</tt>.</li>
 *   <li>If there are few interceptors for one action use <tt>&#064;Order</tt> annotaion for to determine the interceptors order.</li>
 * </ul>
 * 
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
 * 
 * <p><p>Example:
 * <pre>
 * //interceptor
 * &#064;Mapping(SomeAction.class)
 * public class SomeInterceptor extends Interceptor&lt;SomeAction&gt;{
 *   
 *   public void invoke(SomeAction action, InterceptorChain chain) throws Exception {
 *     //before handler
 *     String input = action.input();
 *     String newInput = "changed input [ "+input+" ]";
 *     action.setInput(newInput);
 *     
 *     //next interceptor or handler
 *     chain.doNext();
 *     
 *     //after handler
 *     String output = action.getOutput();
 *     String newOutput = "changed output [ "+output+" ]";
 *     action.setOutput(newOutput);
 *   }
 * }
 * 
 * //handler
 * &#064;Mapping(SomeAction.class)
 * public class SomeHandler extends Handler&lt;SomeAction&gt;{
 * 
 *   public void invoke(SomeAction action) throws Exception {
 *     String input = action.input(); //<---- input is updated by SomeInterceptor
 *     action.setOutput(input);
 *   }
 * }
 * 
 * //engine
 * Engine engine = new Engine();
 * engine.putHandler(SomeHandler.class);
 * engine.putInterceptor(SomeInterceptor.class);
 * String result = engine.invoke(new SomeAction("test"));
 * System.out.println(result); <---- result is "changed output [ changed input [ test ] ]"
 * </pre>
 * 
 *
 * @author Evgeny Dolganov
 * @see Mapping
 * @see Order
 * @see Action
 * @see Handler
 * @see Filter
 * @see easydroid.gf.core.Engine
 */
public abstract class Interceptor<T extends Action<?,?>> extends MappingObject {
	
	/**
	 * Process the action.
	 * @param chain interface to call next interceptor or handler
	 */
	public abstract void invoke(T action, InterceptorChain chain) throws Exception;

}
