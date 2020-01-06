package com.github.jad.jobmanager;


import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import static com.github.jad.utils.CommonUtils.isNonEmpty;

public abstract class AbstractSpringFlow extends AbstractFlow implements DisposableBean {

    private boolean autowireBean = false;
    private String stringBeanName;

    public AbstractSpringFlow() {
    }

    public AbstractSpringFlow(boolean autowireBean) {
        this.autowireBean = autowireBean;
    }

    @Override
    public void destroy() throws Exception {
        try {
            mDoFinalizers.run();
        } catch (Exception e) {
            log.warn("Exception on destroy flow {}", getName(), e);
        }
    }

    @Override
    protected void initializeFlow() {
        ApplicationContext appContext = (ApplicationContext) context.getVariablesMap().get(SpringJobManager.SPRING_CONTEXT);
        if (appContext != null && autowireBean) {
            AutowireCapableBeanFactory factory = appContext.getAutowireCapableBeanFactory();
            factory.autowireBean(this);
        }
    }

    void setStringBeanName(String stringBeanName) {
        this.stringBeanName = stringBeanName;
    }


    @Override
    public String getName() {
        if (isNonEmpty(stringBeanName)) {
            return stringBeanName;
        } else {
            return super.getName();
        }
    }
}
