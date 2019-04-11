/*
 *      Copyright 2019 The original authors.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package io.sundr.it;

import org.junit.Test;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class DefaultTest {

    @Test
    public void shouldReturnNull() {
        Default item = new DefaultBuilder().build();
        assertNull(item.getList());
    }

    @Test
    public void shouldRespectWithNull() {
        Default item = new DefaultBuilder().withList((List<String>)null).build();
        assertNull(item.getList());
    }

    @Test
    public void shouldRespectWithList() {
        Default item = new DefaultBuilder().withList(new ArrayList<String>()).build();
        assertNotNull(item.getList());
        assertEquals(0, item.getList().size());
    }

    @Test
    public void shouldRespecAddToList() {
        Default item = new DefaultBuilder().addToList("foo").build();
        assertNotNull(item.getList());
        assertEquals(1, item.getList().size());
        assertEquals("foo", item.getList().get(0));
    }
}