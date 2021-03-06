/*  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.pluto.container.bean.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.portlet.annotations.RenderStateScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration for RenderStateScoped CDI beans.
 * 
 * @author Scott Nicklous
 *
 */
public class PortletStateScopedConfig  implements Serializable {
   private static final long serialVersionUID = -5333145344722804837L;
   private final Logger LOG = LoggerFactory.getLogger(PortletStateScopedConfig.class);
   private final boolean isTrace = LOG.isTraceEnabled();
   
   
   // Contains a sorted list of RenderStateScoped annotated class names. The sorted list
   // is used to generate indexes for the assigned render parameter names.
   private final List<String> sortedAnnotatedClassNames = new ArrayList<String>();
   
   // Prefix used when generating render parameter names
   private static final String   RP_PREFIX = "\uFE34";

   // Description for the RenderStateScoped bean
   private class PSSDescription implements Serializable {
      private static final long serialVersionUID = 4089751663717085089L;
      RenderStateScoped      pssAnno;
      String                  paramName;
   }
   
   // Maps the bean contextual to the annotation. The bean contextual is obtained
   // during the activation process after all beans have been discovered.
   // Note that synchronization is not needed since the map is only changed during the
   // bean scanning phase.
   private final Map<Contextual<?>, PSSDescription> context2Anno = 
         new HashMap<Contextual<?>, PSSDescription>();

   // Maps the bean class to the corresponding annotation. The entries are set
   // while the extension is processing annotated types.
   // Note that synchronization is not needed since the map is only changed during the
   // bean scanning phase.
   private final Map<Class<?>, PSSDescription> class2Anno = 
         new HashMap<Class<?>, PSSDescription>();
   
   /**
    * Called by the CDI extension during the scanning phase to add information about 
    * a <code>{@literal @}RenderStateScoped</code> bean.
    * 
    * @param beanClass     The bean class
    * @param anno          The annotation
    */
   public void addAnnotation(Class<?> beanClass, RenderStateScoped anno) {
      PSSDescription desc = new PSSDescription();
      desc.pssAnno = anno;
      class2Anno.put(beanClass, desc);
   }
   
   /** 
    * Gets the concrete contextual for the bean and puts it into the context map.
    * Called after bean discovery during the activation process.
    * Finishes up the configuration process and provides a debug summary.
    * 
    * @param bm      The bean manager
    */
   public void activate(BeanManager bm) {
      
      // The assigned render parameters are based on the alphabetic order of the PSS bean 
      // class names, so generate such a list.
      
      for (Class<?> c : class2Anno.keySet()) {
         sortedAnnotatedClassNames.add(c.getCanonicalName());
      }
      Collections.sort(sortedAnnotatedClassNames);
      
      // Now assign the parameter names. If provided through the annotation, use
      // that one. Otherwise generate a render parameter name. 
      
      for (Class<?> c : class2Anno.keySet()) {
         PSSDescription desc = class2Anno.get(c);
         if (desc.pssAnno.paramName().length() > 0) {
            desc.paramName = desc.pssAnno.paramName();
         } else {
            desc.paramName = RP_PREFIX + sortedAnnotatedClassNames.indexOf(c.getCanonicalName());
         }
         
         // Fix up the portlet names if specified by the annotation.
         // prob not needed desc.portletNames.addAll(Arrays.asList(desc.pssAnno.portletNames()));
      }
      
      // Activate the beans
      
      for (Class<?> cls : class2Anno.keySet()) {
         Set<Bean<?>> beans = bm.getBeans(cls);
         Bean<?> bean = bm.resolve(beans);
         assert bean != null;
         context2Anno.put(bean, class2Anno.get(cls));
      }
      
      // dump configuration data to trace
      
      if (isTrace) {
         StringBuilder txt = new StringBuilder(128);
         txt.append("PortletStateScopedBeanHolder configuration.");
         txt.append(" Annotated Beans: ");
         txt.append(getConfigAsString());
         LOG.debug(txt.toString());
      }
   }
   
   /**
    * Returns the render state scoped annotated classes. 
    * <p>
    * Used for test / validation purposes.
    * 
    * @return  Set of annotated classes
    */
   public Set<Class<?>> getBeanClasses() {
      return class2Anno.keySet();
   }
   
   /**
    * Returns a render state scoped bean summary for display
    * 
    * @return  The configuration summary string
    * 
    */
   public String getConfigAsString() {
      StringBuilder txt = new StringBuilder(128);
      for (Class<?> c : class2Anno.keySet()) {
         txt.append("\n\tClass: ").append(c.getCanonicalName());
         PSSDescription desc = class2Anno.get(c);
         txt.append(", Param name: ").append(desc.paramName);
      }
      return txt.toString();
   }
   
   /**
    * Returns the parameter name for the given bean class.
    * 
    * @param beanClass  The bean class
    * @return           The corresponding parameter name
    */
   public String getParamName(Class<?> beanClass) {
      String name = null;
      for (Contextual<?> b : context2Anno.keySet()) {
         if (b instanceof Bean) {
            Bean<?> bean = (Bean<?>)b;
            if (beanClass.isAssignableFrom(bean.getBeanClass()) || bean.getBeanClass().isAssignableFrom(beanClass)) {
               name = context2Anno.get(b).paramName;
               break;
            }
         }
      }
      return name;
   }
   
   /**
    * Determines the render parameter name for the given bean.
    * 
    * @param bean    The bean
    * @return        The parameter name
    */
   public String getParamName(Bean<?> bean) {
      
      // retrieve the annotation for the bean. If a parameter name is provided
      // use it. Otherwise use the class name.
      
      PSSDescription desc = context2Anno.get(bean);
      assert desc != null;
      assert desc.paramName.length() > 0;
      return desc.paramName;
   }

}
