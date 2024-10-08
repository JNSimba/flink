/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.table.api.typeutils

import org.apache.flink.api.common.io.FileInputFormat
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.api.java.typeutils.{PojoTypeInfo, ResultTypeQueryable}
import org.apache.flink.core.fs
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.typeutils.TypeExtractionTest.{CustomBeanClass, CustomTypeInputFormat}

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path

import scala.beans.BeanProperty

class TypeExtractionTest {

  @Test
  def testResultTypeQueryable(@TempDir tempDir: Path): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val producedType =
      env.createInput(new CustomTypeInputFormat(tempDir.toAbsolutePath.toString)).getType()
    assertThat(producedType).isEqualTo(BasicTypeInfo.LONG_TYPE_INFO)
  }

  @Test
  def testBeanPropertyClass(): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val producedType = env.fromElements(new CustomBeanClass()).getType()
    assertThat(producedType).isInstanceOf(classOf[PojoTypeInfo[_]])
    val pojoTypeInfo = producedType.asInstanceOf[PojoTypeInfo[_]]
    assertThat(pojoTypeInfo.getTypeAt(0)).isEqualTo(BasicTypeInfo.INT_TYPE_INFO)
    assertThat(pojoTypeInfo.getTypeAt(1)).isEqualTo(BasicTypeInfo.LONG_TYPE_INFO)
  }

}

object TypeExtractionTest {
  class CustomTypeInputFormat(tempDir: String)
    extends FileInputFormat[String]
    with ResultTypeQueryable[Long] {

    override def getProducedType: TypeInformation[Long] =
      BasicTypeInfo.LONG_TYPE_INFO.asInstanceOf[TypeInformation[Long]]

    override def reachedEnd(): Boolean = throw new UnsupportedOperationException()

    override def nextRecord(reuse: String): String = throw new UnsupportedOperationException()

    override def getFilePaths: Array[fs.Path] = {
      Array(new fs.Path(tempDir))
    }
  }

  class CustomBeanClass(@BeanProperty var prop: Int, var prop2: Long) {
    def this() = this(0, 0L)
  }
}
