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

package io.sundr.builder.internal.functions;

import io.sundr.Function;
import io.sundr.FunctionFactory;
import io.sundr.builder.annotations.FilterDescendants;
import io.sundr.builder.annotations.IgnoreDescendants;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.builder.internal.visitors.InitEnricher;
import io.sundr.codegen.model.AnnotationRef;
import io.sundr.codegen.model.ClassRef;
import io.sundr.codegen.model.ClassRefBuilder;
import io.sundr.codegen.model.Kind;
import io.sundr.codegen.model.Property;
import io.sundr.codegen.model.PropertyBuilder;
import io.sundr.codegen.model.TypeDef;
import io.sundr.codegen.model.TypeRef;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.sundr.builder.Constants.BUILDABLE_ENABLED;
import static io.sundr.builder.Constants.DESCENDANT_OF;
import static io.sundr.builder.Constants.GENERATED;
import static io.sundr.builder.Constants.ORIGIN_TYPEDEF;
import static io.sundr.codegen.functions.Collections.IS_COLLECTION;
import static io.sundr.codegen.functions.Collections.IS_MAP;
import static io.sundr.codegen.utils.StringUtils.compact;
import static io.sundr.codegen.utils.StringUtils.deCapitalizeFirst;

class Descendants {

    private static final String VALUE = "value";

    private Descendants() {
    }

    private static final Function<TypeDef, Set<TypeDef>> BUILDABLE_DESCENDANTS = FunctionFactory
        .cache(
            (Function<TypeDef, Set<TypeDef>>) item -> {
                if (item.equals(TypeDef.OBJECT)) {
                    return Collections.emptySet();
                }
                return BuilderContextManager.getContext().getBuildableRepository().getBuildables()
                    .stream()
                    .filter(type -> type.getKind() == Kind.CLASS)
                    .filter(type -> !type.isAbstract())
                    .filter(type -> isDescendant(type, item))
                    .filter(type -> !type.equals(item))
                    .filter(type -> !type.hasAttribute(GENERATED))
                    .collect(Collectors.toSet());
            },
            (typeDef, typeDefSet) ->
                Optional.ofNullable(typeDef).map(TypeDef::getFullyQualifiedName).orElse(null)
        ).withMaximumRecursionLevel(2).withMaximumNestingDepth(2);

    /**
     * Find all buildable descendant equivalents of a property.
     */
    static Function<Property, Set<Property>> PROPERTY_BUILDABLE_DESCENDANTS = FunctionFactory.wrap(new Function<Property, Set<Property>>() {
        public Set<Property> apply(Property property) {
            if (isNestingIgnored(property)) {
                return Collections.emptySet();
            }
            Set<Property> result = new LinkedHashSet<Property>();

            TypeRef baseType = property.getTypeRef();
            TypeDef origin = property.getAttribute(ORIGIN_TYPEDEF);

            if (IS_COLLECTION.apply(baseType)) {
                TypeRef unwrapped = TypeAs.UNWRAP_COLLECTION_OF.apply(baseType);
                if (unwrapped instanceof  ClassRef) {
                    ClassRef candidate = (ClassRef) unwrapped;

                    for (TypeDef descendant : BUILDABLE_DESCENDANTS.apply(candidate.getDefinition())) {
                        ClassRef descendantRef = new ClassRefBuilder(descendant.toInternalReference())
                                .build();

                        if (isNestingFiltered(property, descendantRef)) {
                            continue;
                        } else if (origin.getName().equals(descendant.getName()) && !origin.getPackageName().equals(descendant.getPackageName())) {
                            //We don't want to have a class that references a descendant with the same name in an other package. It's an extreme case and will not work.
                            continue;
                        }

                        ClassRef collectionType = new ClassRefBuilder((ClassRef)baseType)
                                .withArguments(descendantRef)
                                .build();

                        String propertyName = compact(deCapitalizeFirst(descendant.getName()) + property.getNameCapitalized());
                        result.add(new PropertyBuilder(property)
                                .withName(propertyName)
                                .withTypeRef(collectionType)
                                .addToAttributes(DESCENDANT_OF, property)
                                .addToAttributes(BUILDABLE_ENABLED, true)
                                .accept(new InitEnricher())
                                .build());
                    }
                }
            } else if (IS_MAP.apply(baseType)) {
                TypeRef unwrapped = TypeAs.UNWRAP_MAP_VALUE_OF.apply(baseType);
                if (unwrapped instanceof  ClassRef) {
                    ClassRef candidate = (ClassRef) unwrapped;

                    for (TypeDef descendant : BUILDABLE_DESCENDANTS.apply(candidate.getDefinition())) {
                        ClassRef descendantRef = new ClassRefBuilder(descendant.toInternalReference())
                                .build();

                        if (isNestingFiltered(property, descendantRef)) {
                            continue;
                        } else if (origin.getName().equals(descendant.getName()) && !origin.getPackageName().equals(descendant.getPackageName())) {
                            //We don't want to have a class that references a descendant with the same name in an other package. It's an extreme case and will not work.
                            continue;
                        }

                        ClassRef mapType = new ClassRefBuilder((ClassRef)baseType)
                                .withArguments(TypeAs.UNWRAP_MAP_KEY_OF.apply(baseType), descendantRef)
                                .build();

                        String propertyName = compact(deCapitalizeFirst(descendant.getName()) + property.getNameCapitalized());
                        result.add(new PropertyBuilder(property)
                                .withName(propertyName)
                                .withTypeRef(mapType)
                                .addToAttributes(DESCENDANT_OF, property)
                                .addToAttributes(BUILDABLE_ENABLED, true)
                                .accept(new InitEnricher())
                                .build());
                    }
                }
            } else if (baseType instanceof  ClassRef) {
                ClassRef candidate = (ClassRef) baseType;
                for (TypeDef descendant : BUILDABLE_DESCENDANTS.apply(candidate.getDefinition())) {
                    ClassRef descendantRef = new ClassRefBuilder(descendant.toInternalReference())
                            .build();

                    if (isNestingFiltered(property, descendantRef)) {
                        continue;
                    } else if (origin.getName().equals(descendant.getName()) && !origin.getPackageName().equals(descendant.getPackageName())) {
                        //We don't want to have a class that references a descendant with the same name in an other package. It's an extreme case and will not work.
                        continue;
                    }

                    String propertyName =  compact(deCapitalizeFirst(descendant.getName() + property.getNameCapitalized()));
                    result.add(new PropertyBuilder(property)
                            .withName(propertyName)
                            .withTypeRef(descendantRef)
                            .addToAttributes(DESCENDANT_OF, property)
                            .addToAttributes(BUILDABLE_ENABLED, true)
                            .accept(new InitEnricher())
                            .build());
                }
            }
            return result;
        }
    });

    /**
     * Checks if a type is an descendant of an other type
     *
     * @param item      The base type.
     * @param candidate The candidate type.
     * @return true if candidate is a descendant of base type.
     */
    public static boolean isDescendant(TypeDef item, TypeDef candidate) {
        if (item == null || candidate == null) {
            return false;
        } else if (candidate.isAssignableFrom(item)) {
            return true;
        }
        return false;
    }

    public static boolean isNestingIgnored(Property property) {
        for (AnnotationRef ref : property.getAnnotations()) {
            if (ref.getClassRef().getFullyQualifiedName().equals(IgnoreDescendants.class.getName())) {
                return true;
            }
        }
        return false;
    }


   public static boolean isNestingFiltered(Property property, ClassRef classRef) {
        for (AnnotationRef ref : property.getAnnotations()) {
            if (ref.getClassRef().getFullyQualifiedName().equals(FilterDescendants.class.getName())) {
                Map<String, Object> parameters = ref.getParameters();
                Object value  = parameters == null ? null : parameters.get(VALUE);
                if (value instanceof String && property.getTypeRef() instanceof ClassRef) {
                    Pattern p = Pattern.compile((String) value);
                    if (p.matcher(classRef.getFullyQualifiedName()).matches()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }




}
