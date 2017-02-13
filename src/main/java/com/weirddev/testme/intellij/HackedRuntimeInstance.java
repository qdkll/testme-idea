package com.weirddev.testme.intellij;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.diagnostic.Logger;
import com.weirddev.testme.intellij.template.FTFolderManager;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Motivation: use a hidden folder where included velocity templates can be referenced. the default IJ includes folder can be overridden by the user. overriding the included macros will break plugin functionality on upgrades
 * Implementation: an ugly hack on velocity inner implementation.
 * Reasoning: Velocity is statically initialized, globally, in IJ IDEA. Extending IJ Classes that initialize Velocity is tricky since their API keeps changing between versions 14 - 2016
 *
 * Date: 2/13/2017
 * @author Yaron Yamin
 */

public class HackedRuntimeInstance extends RuntimeInstance {
    private static final Logger LOG = Logger.getInstance(HackedRuntimeInstance.class.getName());
    private FTFolderManager ftFolderManager = new FTFolderManager("fileTemplates/testMeIncludes");

    @Override
    public void setProperty(String key, Object value) {
        if ("resource.loader".equals(key) && value instanceof String && !((String) value).contains("testMeIncludes")) {
            setTestMeVelocityIncludesLoader(key, value);
        } else {
            super.setProperty(key, value);
        }
    }

    private void setTestMeVelocityIncludesLoader(String key, Object value) {
        LOG.debug("setting TestMe includes dir in Velocity");
        super.setProperty(key, value + ",testMeIncludes");
        super.setProperty("testMeIncludes.resource.loader.instance", new ResourceLoader() {
            @Override
            public void init(ExtendedProperties configuration) {
            }

            @Override
            public InputStream getResourceStream(String resourceName) throws ResourceNotFoundException {
                final FileTemplate include = ftFolderManager.getTemplates().get(resourceName);
                if (include == null) {
                    throw new ResourceNotFoundException("Template not found: " + resourceName);
                }
                final String text = include.getText();
                try {
                    return new ByteArrayInputStream(text.getBytes(FileTemplate.ourEncoding));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean isSourceModified(Resource resource) {
                return true;
            }

            @Override
            public long getLastModified(Resource resource) {
                return 0L;
            }
        });
    }
}
