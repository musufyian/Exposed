package org.jetbrains.exposed.sql.tests.shared.ddl

import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.tests.DatabaseTestsBase
import org.jetbrains.exposed.sql.tests.TestDB
import org.jetbrains.exposed.sql.tests.shared.assertEquals
import org.jetbrains.exposed.sql.tests.shared.assertTrue
import org.junit.Test

class CreateIndexTests : DatabaseTestsBase() {

    @Test
    fun createStandardIndex() {
        val TestTable = object : Table("test_table") {
            val id = integer("id")
            val name = varchar("name", length = 42)

            override val primaryKey = PrimaryKey(id)
            val byName = index("test_table_by_name", false, name)
        }

        withTables(excludeSettings = listOf(TestDB.Jdbc.H2_MYSQL), tables = arrayOf(TestTable)) {
            SchemaUtils.createMissingTablesAndColumns(TestTable)
            assertTrue(TestTable.exists())
            SchemaUtils.drop(TestTable)
        }
    }

    @Test
    fun createHashIndex() {
        val TestTable = object : Table("test_table") {
            val id = integer("id")
            val name = varchar("name", length = 42)

            override val primaryKey = PrimaryKey(id)
            val byNameHash = index("test_table_by_name", /* isUnique = */ false, name, indexType = "HASH")
        }

        withTables(excludeSettings = listOf(TestDB.Jdbc.H2_MYSQL, TestDB.Jdbc.SQLSERVER, TestDB.Jdbc.ORACLE), tables = arrayOf(TestTable)) {
            SchemaUtils.createMissingTablesAndColumns(TestTable)
            assertTrue(TestTable.exists())
        }
    }

    @Test
    fun createNonClusteredSQLServerIndex() {
        val TestTable = object : Table("test_table") {
            val id = integer("id")
            val name = varchar("name", length = 42)

            override val primaryKey = PrimaryKey(id)
            val byNameHash = index("test_table_by_name", /* isUnique = */ false, name, indexType = "NONCLUSTERED")
        }

        withDb(TestDB.Jdbc.SQLSERVER) {
            SchemaUtils.createMissingTablesAndColumns(TestTable)
            assertTrue(TestTable.exists())
            SchemaUtils.drop(TestTable)
        }
    }

    @Test
    fun `test possibility to create indexes when table exists in defferent schemas`() {
        val TestTable = object : Table("test_table") {
            val id = integer("id").uniqueIndex()
            val name = varchar("name", length = 42).index("test_index")
            init {
                index(false, id, name)
            }
        }
        val schema1 = Schema("Schema1")
        val schema2 = Schema("Schema2")
        val tests = TestDB.values().filterIsInstance<TestDB.Rdbc>() + listOf(TestDB.Jdbc.SQLITE, TestDB.Jdbc.SQLSERVER)
        withSchemas(tests, schema1, schema2) {
            SchemaUtils.setSchema(schema1)
            SchemaUtils.createMissingTablesAndColumns(TestTable)
            assertEquals(true, TestTable.exists())
            SchemaUtils.setSchema(schema2)
            assertEquals(false, TestTable.exists())
            SchemaUtils.createMissingTablesAndColumns(TestTable)
            assertEquals(true, TestTable.exists())
        }
    }
}