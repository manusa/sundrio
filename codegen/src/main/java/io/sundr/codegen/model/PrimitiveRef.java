/*
 * Copyright 2016 The original authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.sundr.codegen.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Map;

public class PrimitiveRef extends TypeRef {

    private final String name;
    private final int dimensions;

    public PrimitiveRef(String name, int dimensions, Map<AttributeKey, Object> attributes) {
        super(attributes);
        this.name = name;
        this.dimensions = dimensions;
    }

    public String getName() {
        return name;
    }

    public int getDimensions() {
        return dimensions;
    }

    public PrimitiveRef withDimensions(int dimensions) {
        return new PrimitiveRefBuilder(this).withDimensions(dimensions).build();
    }

    public boolean isAssignableFrom(TypeRef o) {
        if (this == o) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof ClassRef) {
            if (!((ClassRef) o).getDefinition().getPackageName().equals(JAVA_LANG)) {
                return false;
            }
            if (!((ClassRef) o).getDefinition().getName().toUpperCase().startsWith(name.toUpperCase())) {
                return false;
            }
            return true;
        }

        if (o == null || getClass() != o.getClass()) return false;

        PrimitiveRef that = (PrimitiveRef) o;

        if (dimensions != that.dimensions) return false;
        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PrimitiveRef that = (PrimitiveRef) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(dimensions, that.dimensions)
            .append(name, that.name)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(name)
            .append(dimensions)
            .toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        for (int i = 0; i < dimensions; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

}
