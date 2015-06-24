/*
 * Copyright (C) 2012-2015 the original author or authors.
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
package ninja.servlet;

import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ninja.bodyparser.BodyParserEngineManager;
import ninja.servlet.file.NinjaFileItemStreamFactory;
import ninja.session.FlashScope;
import ninja.session.Session;
import ninja.utils.NinjaConstant;
import ninja.utils.NinjaMode;
import ninja.utils.NinjaProperties;
import ninja.utils.NinjaPropertiesImpl;
import ninja.utils.ResultHandler;
import ninja.validation.Validation;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultipartContextImplTest {

    @Mock
    private Session sessionCookie;

    @Mock
    private FlashScope flashCookie;

    @Mock
    private BodyParserEngineManager bodyParserEngineManager;

    @Mock
    private ServletContext servletContext;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private ResultHandler resultHandler;

    @Mock
    private Validation validation;

    private NinjaProperties ninjaProperties;

    private MultipartContextImpl context;

    private String paramA = "paramA";
    private String paramB = "paramB";
    private String valueA = "valueA";
    private String valueB1 = "valueB1";
    private String valueB2 = "valueB2";

    private String file1 = "file1";
    private String file2 = "file2";
    private String file1Name = "my-file1.txt";
    private String file2Name = "my-file2.txt";
    private String file1Data = "abcdefghijklmnopqrstuvwxyz";
    private String file2Data = "1234567890";

    @Before
    public void setUp() throws IOException, ServletException {
        //default setup for httpServlet request.
        //According to servlet spec the following will be returned:
        when(httpServletRequest.getContextPath()).thenReturn("");
        when(httpServletRequest.getRequestURI()).thenReturn("/");

        NinjaPropertiesImpl properties = new NinjaPropertiesImpl(NinjaMode.test);
        properties.setProperty(NinjaConstant.FILE_UPLOADS_IN_MEMORY, "false");
        this.ninjaProperties = properties;

        MultipartContextImplWithFileItems ctx = new MultipartContextImplWithFileItems(
                bodyParserEngineManager,
                flashCookie,
                ninjaProperties,
                resultHandler,
                sessionCookie,
                validation);
        ctx.fileItemIterator = makeFileItemsIterator();
        this.context = ctx;
        this.context.fileItemStreamFactory = new NinjaFileItemStreamFactory(ninjaProperties);
    }

    @After
    public void cleanUp() {
        if (context instanceof ContextImpl) {
            ((ContextImpl) context).cleanup();
        }
    }

    private FileItemIterator makeFileItemsIterator() {

        List<FileItemStream> fileItems = new ArrayList<>();
        FileItemStreamImpl item;

        // ===== uploaded file items =====
        item = new FileItemStreamImpl(file1, "text/plain", file1Name);
        item.data = file1Data.getBytes();
        fileItems.add(item);

        item = new FileItemStreamImpl(file2, "text/plain", file2Name);
        item.data = file2Data.getBytes();
        fileItems.add(item);

        // ===== simple key-value form fields =====
        Map<String, List<String>> params = new HashMap<>();
        params.put(paramA, Arrays.asList(valueA));
        params.put(paramB, Arrays.asList(valueB1, valueB2));

        return new MultipartContextImpl.FileItemIteratorImpl(fileItems, params);
    }

    @Test
    public void testGetParameter() {

        context.init(servletContext, httpServletRequest, httpServletResponse);

        Assert.assertEquals(valueA, context.getParameter(paramA));
        Assert.assertEquals(valueB1, context.getParameter(paramB));
        Assert.assertNull(context.getParameter("paramX"));
    }

    @Test
    public void testGetParameterValues() {

        context.init(servletContext, httpServletRequest, httpServletResponse);

        List<String> params = context.getParameterValues(paramA);
        Assert.assertEquals(1, params.size());
        Assert.assertTrue(params.contains(valueA));

        params = context.getParameterValues(paramB);
        Assert.assertEquals(2, params.size());
        Assert.assertTrue(params.contains(valueB1));
        Assert.assertTrue(params.contains(valueB2));
    }

    @Test
    public void testGetParameters() {

        context.init(servletContext, httpServletRequest, httpServletResponse);

        Map<String, String[]> params = context.getParameters();

        Assert.assertEquals(2, params.size());

        String[] arr = params.get(paramA);
        Assert.assertNotNull(arr);
        Assert.assertEquals(1, arr.length);
        Assert.assertEquals(valueA, arr[0]);

        arr = params.get(paramB);
        Assert.assertNotNull(arr);
        Assert.assertEquals(2, arr.length);
        Assert.assertEquals(valueB1, arr[0]);
        Assert.assertEquals(valueB2, arr[1]);

    }

    @Test
    public void testIsMultipart() {
        Assert.assertTrue(context.isMultipart());
    }

    @Test
    public void testGetUploadedFileStream() throws IOException {

        context.init(servletContext, httpServletRequest, httpServletResponse);

        try (InputStream is = context.getUploadedFileStream(file1)) {

            Assert.assertNotNull(is);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                Assert.assertEquals(file1Data, sb.toString());
            }
        }

        Assert.assertNotNull(context.getUploadedFileStream(file2));
        Assert.assertNull(context.getUploadedFileStream("fileX"));
    }

    @Test
    public void testGetUploadedFileStreams() {

        context.init(servletContext, httpServletRequest, httpServletResponse);

        List<InputStream> files = context.getUploadedFileStreams(file1);
        Assert.assertEquals(1, files.size());

        // empty collection for nonexisting file
        files = context.getUploadedFileStreams("fileX");
        Assert.assertNotNull(files);
        Assert.assertTrue(files.isEmpty());
    }

    @Test
    public void testGetFileItems() {

        context.init(servletContext, httpServletRequest, httpServletResponse);

        List<FileItemStream> items = context.getFileItems();
        Assert.assertEquals(2, items.size());
    }

    /**
     * Extended multipart context implementation with mocking file item iterator
     * provided.
     */
    private static class MultipartContextImplWithFileItems extends MultipartContextImpl {

        FileItemIterator fileItemIterator;

        public MultipartContextImplWithFileItems(
                BodyParserEngineManager bodyParserEngineManager,
                FlashScope flashCookie,
                NinjaProperties ninjaProperties,
                ResultHandler resultHandler,
                Session sessionCookie,
                Validation validation) {

            super(bodyParserEngineManager,
                    flashCookie,
                    ninjaProperties,
                    resultHandler,
                    sessionCookie,
                    validation);
        }

        @Override
        void parseParts() {
            // pass our mocking file item iterator
            super.parseParts(this.fileItemIterator);
        }

    }

    /**
     * Simple file item stream impl to mock uploaded file items.
     */
    private static class FileItemStreamImpl implements FileItemStream {

        String fieldName;
        String contentType;
        String name;
        byte[] data;

        public FileItemStreamImpl(String fieldName, String contentType, String name) {
            this.fieldName = fieldName;
            this.contentType = contentType;
            this.name = name;
        }

        @Override
        public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public boolean isFormField() {
            return false;
        }

        @Override
        public FileItemHeaders getHeaders() {
            throw new UnsupportedOperationException("Not supported for tests");
        }

        @Override
        public void setHeaders(FileItemHeaders headers) {
            throw new UnsupportedOperationException("Not supported for tests");
        }

    }

}