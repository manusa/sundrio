/*
 * Copyright 2015 The original authors.
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

package io.sundr.builder.internal.processor;

import io.sundr.builder.Visitor;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.internal.BuilderContext;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.builder.internal.utils.BuilderUtils;
import io.sundr.codegen.functions.ElementTo;
import io.sundr.codegen.model.PropertyBuilder;
import io.sundr.codegen.model.TypeDef;
import io.sundr.codegen.model.TypeDefBuilder;
import io.sundr.codegen.utils.ModelUtils;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static io.sundr.builder.Constants.BUILDABLE;
import static io.sundr.builder.Constants.EDITABLE_ENABLED;
import static io.sundr.builder.Constants.LAZY_COLLECTIONS_INIT_ENABLED;
import static io.sundr.builder.Constants.LAZY_MAP_INIT_ENABLED;
import static io.sundr.builder.Constants.VALIDATION_ENABLED;

@SupportedAnnotationTypes("io.sundr.builder.annotations.Buildable")
public class BuildableProcessor extends AbstractBuilderProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();

        BuilderContext ctx = null;

        //First pass register all buildables
        final Queue<TypeDef> buildables = new LinkedList<>();
        for (TypeElement typeElement : annotations) {
            for (Element element : env.getElementsAnnotatedWith(typeElement)) {
                Buildable buildable = element.getAnnotation(Buildable.class);
                if (buildable == null) {
                    continue;
                }

                ctx = BuilderContextManager.create(elements, types, buildable.validationEnabled(), buildable.generateBuilderPackage(), buildable.builderPackage());
                        TypeDef b = new TypeDefBuilder(ElementTo.TYPEDEF.apply(ModelUtils.getClassElement(element)))
                                .addToAttributes(BUILDABLE, buildable)
                                .addToAttributes(EDITABLE_ENABLED, buildable.editableEnabled())
                                .addToAttributes(VALIDATION_ENABLED, buildable.validationEnabled())
                                .accept(new Visitor<PropertyBuilder>() {
                                    @Override
                                    public void visit(PropertyBuilder builder) {
                                       builder.addToAttributes(LAZY_COLLECTIONS_INIT_ENABLED, buildable.lazyCollectionInitEnabled());
                                       builder.addToAttributes(LAZY_MAP_INIT_ENABLED, buildable.lazyMapInitEnabled());
                                    }
                                }).build();

                    ctx.getDefinitionRepository().register(b);
                    ctx.getBuildableRepository().register(b);
                    buildables.add(b);

                for (TypeElement ref : BuilderUtils.getBuildableReferences(ctx, buildable)) {
                    TypeDef r = new TypeDefBuilder(ElementTo.TYPEDEF.apply(ModelUtils.getClassElement(ref)))
                            .addToAttributes(BUILDABLE, buildable)
                            .addToAttributes(EDITABLE_ENABLED, buildable.editableEnabled())
                            .addToAttributes(VALIDATION_ENABLED, buildable.validationEnabled())
                            .accept(new Visitor<PropertyBuilder>() {
                                @Override
                                public void visit(PropertyBuilder builder) {
                                    builder.addToAttributes(LAZY_COLLECTIONS_INIT_ENABLED, buildable.lazyCollectionInitEnabled());
                                    builder.addToAttributes(LAZY_MAP_INIT_ENABLED, buildable.lazyMapInitEnabled());
                                }
                            }).build();

                    ctx.getDefinitionRepository().register(r);
                    ctx.getBuildableRepository().register(r);
                    buildables.add(r);
                }
            }
        }

        if (ctx == null) {
            return true;
        }
        generateLocalDependenciesIfNeeded();
        addCustomMappings(ctx);

        ctx.getDefinitionRepository().updateReferenceMap();
        final int total = ctx.getBuildableRepository().getCount();
        int count = 0;
        TypeDef typeDef;
        while((typeDef = buildables.poll()) != null){
            double percentage = 100 * (count++) / total;
            System.err.println(Math.round(percentage)+"%: " + typeDef.getFullyQualifiedName());
            generateBuildable(ctx, typeDef);
            generatePojos(ctx, typeDef);
        }
        System.err.println("100%: Builder generation complete.");
        return false;
    }
}
