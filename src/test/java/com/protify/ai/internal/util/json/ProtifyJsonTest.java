/*
 * Copyright(c) 2026 Protify Consulting LLC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.protify.ai.internal.util.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProtifyJsonTest {

    // ---------------------------------------------------------------
    // Test model classes
    // ---------------------------------------------------------------

    public static class SimpleBean {
        private String name;
        private int age;

        public SimpleBean() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class AllPrimitives {
        private int intVal;
        private long longVal;
        private double doubleVal;
        private float floatVal;
        private boolean boolVal;
        private short shortVal;
        private byte byteVal;

        public AllPrimitives() {}

        public int getIntVal() { return intVal; }
        public void setIntVal(int intVal) { this.intVal = intVal; }
        public long getLongVal() { return longVal; }
        public void setLongVal(long longVal) { this.longVal = longVal; }
        public double getDoubleVal() { return doubleVal; }
        public void setDoubleVal(double doubleVal) { this.doubleVal = doubleVal; }
        public float getFloatVal() { return floatVal; }
        public void setFloatVal(float floatVal) { this.floatVal = floatVal; }
        public boolean isBoolVal() { return boolVal; }
        public void setBoolVal(boolean boolVal) { this.boolVal = boolVal; }
        public short getShortVal() { return shortVal; }
        public void setShortVal(short shortVal) { this.shortVal = shortVal; }
        public byte getByteVal() { return byteVal; }
        public void setByteVal(byte byteVal) { this.byteVal = byteVal; }
    }

    public static class WrapperTypes {
        private Integer intVal;
        private Long longVal;
        private Double doubleVal;
        private Float floatVal;
        private Boolean boolVal;

        public WrapperTypes() {}

        public Integer getIntVal() { return intVal; }
        public void setIntVal(Integer intVal) { this.intVal = intVal; }
        public Long getLongVal() { return longVal; }
        public void setLongVal(Long longVal) { this.longVal = longVal; }
        public Double getDoubleVal() { return doubleVal; }
        public void setDoubleVal(Double doubleVal) { this.doubleVal = doubleVal; }
        public Float getFloatVal() { return floatVal; }
        public void setFloatVal(Float floatVal) { this.floatVal = floatVal; }
        public Boolean getBoolVal() { return boolVal; }
        public void setBoolVal(Boolean boolVal) { this.boolVal = boolVal; }
    }

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    public static class EnumBean {
        private String label;
        private Severity severity;

        public EnumBean() {}

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Severity getSeverity() { return severity; }
        public void setSeverity(Severity severity) { this.severity = severity; }
    }

    public static class Address {
        private String street;
        private String city;
        private int zip;

        public Address() {}

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public int getZip() { return zip; }
        public void setZip(int zip) { this.zip = zip; }
    }

    public static class Person {
        private String name;
        private Address address;

        public Person() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
    }

    public static class WithList {
        private String title;
        private List<String> tags;

        public WithList() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class WithObjectList {
        private String name;
        private List<Address> addresses;

        public WithObjectList() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Address> getAddresses() { return addresses; }
        public void setAddresses(List<Address> addresses) { this.addresses = addresses; }
    }

    public static class WithMap {
        private String label;
        private Map<String, Integer> scores;

        public WithMap() {}

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Map<String, Integer> getScores() { return scores; }
        public void setScores(Map<String, Integer> scores) { this.scores = scores; }
    }

    public static class WithObjectMap {
        private Map<String, Address> offices;

        public WithObjectMap() {}

        public Map<String, Address> getOffices() { return offices; }
        public void setOffices(Map<String, Address> offices) { this.offices = offices; }
    }

    public static class AnnotatedBean {
        @ProtifyJsonProperty("full_name")
        private String fullName;

        @ProtifyJsonProperty("star_count")
        private int starCount;

        private String description;

        public AnnotatedBean() {}

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public int getStarCount() { return starCount; }
        public void setStarCount(int starCount) { this.starCount = starCount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class BaseEntity {
        private String id;

        public BaseEntity() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public static class ExtendedEntity extends BaseEntity {
        private String value;

        public ExtendedEntity() {}

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class FieldOnlyBean {
        private String secret;
        private int count;

        public FieldOnlyBean() {}

        public String getSecret() { return secret; }
        public int getCount() { return count; }
        // no setters — fromJson must use field.setAccessible
    }

    public static class DeeplyNested {
        private String label;
        private WithObjectList data;

        public DeeplyNested() {}

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public WithObjectList getData() { return data; }
        public void setData(WithObjectList data) { this.data = data; }
    }

    public static class WithStringArray {
        private String title;
        private String[] tags;

        public WithStringArray() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }
    }

    public static class WithIntArray {
        private int[] values;

        public WithIntArray() {}

        public int[] getValues() { return values; }
        public void setValues(int[] values) { this.values = values; }
    }

    public static class WithObjectArray {
        private String name;
        private Address[] addresses;

        public WithObjectArray() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Address[] getAddresses() { return addresses; }
        public void setAddresses(Address[] addresses) { this.addresses = addresses; }
    }

    public static class FinancialRecord {
        private String currency;
        private BigDecimal amount;
        private BigDecimal rate;
        private BigInteger transactionId;

        public FinancialRecord() {}

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }
        public BigInteger getTransactionId() { return transactionId; }
        public void setTransactionId(BigInteger transactionId) { this.transactionId = transactionId; }
    }

    public static class MixedNumericBean {
        private BigDecimal price;
        private BigInteger quantity;
        private int count;
        private double ratio;

        public MixedNumericBean() {}

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigInteger getQuantity() { return quantity; }
        public void setQuantity(BigInteger quantity) { this.quantity = quantity; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getRatio() { return ratio; }
        public void setRatio(double ratio) { this.ratio = ratio; }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — basic types
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — basic object deserialization")
    class FromJsonBasic {

        @Test
        @DisplayName("Should deserialize simple string and int fields")
        void testSimpleBean() {
            String json = "{\"name\":\"Alice\",\"age\":30}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("Alice", bean.getName());
            assertEquals(30, bean.getAge());
        }

        @Test
        @DisplayName("Should handle whitespace and formatting in JSON")
        void testFormattedJson() {
            String json = "{\n  \"name\" : \"Bob\",\n  \"age\" : 25\n}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("Bob", bean.getName());
            assertEquals(25, bean.getAge());
        }

        @Test
        @DisplayName("Should leave missing fields at default values")
        void testMissingFields() {
            String json = "{\"name\":\"Charlie\"}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("Charlie", bean.getName());
            assertEquals(0, bean.getAge());
        }

        @Test
        @DisplayName("Should ignore extra JSON keys not present on the class")
        void testExtraFields() {
            String json = "{\"name\":\"Dana\",\"age\":40,\"email\":\"dana@test.com\"}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("Dana", bean.getName());
            assertEquals(40, bean.getAge());
        }

        @Test
        @DisplayName("Should handle null values in JSON")
        void testNullValue() {
            String json = "{\"name\":null,\"age\":10}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertNull(bean.getName());
            assertEquals(10, bean.getAge());
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — primitive and wrapper types
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — primitive and wrapper types")
    class FromJsonPrimitives {

        @Test
        @DisplayName("Should deserialize all primitive types")
        void testAllPrimitives() {
            String json = "{\"intVal\":42,\"longVal\":9999999999,\"doubleVal\":3.14,"
                    + "\"floatVal\":1.5,\"boolVal\":true,\"shortVal\":7,\"byteVal\":3}";
            AllPrimitives bean = ProtifyJson.fromJson(json, AllPrimitives.class);

            assertEquals(42, bean.getIntVal());
            assertEquals(9999999999L, bean.getLongVal());
            assertEquals(3.14, bean.getDoubleVal(), 0.001);
            assertEquals(1.5f, bean.getFloatVal(), 0.001f);
            assertTrue(bean.isBoolVal());
            assertEquals((short) 7, bean.getShortVal());
            assertEquals((byte) 3, bean.getByteVal());
        }

        @Test
        @DisplayName("Should deserialize wrapper types")
        void testWrapperTypes() {
            String json = "{\"intVal\":100,\"longVal\":200,\"doubleVal\":1.1,\"floatVal\":2.2,\"boolVal\":false}";
            WrapperTypes bean = ProtifyJson.fromJson(json, WrapperTypes.class);

            assertEquals(Integer.valueOf(100), bean.getIntVal());
            assertEquals(Long.valueOf(200), bean.getLongVal());
            assertEquals(1.1, bean.getDoubleVal(), 0.001);
            assertEquals(2.2f, bean.getFloatVal(), 0.001f);
            assertFalse(bean.getBoolVal());
        }

        @Test
        @DisplayName("Should leave wrapper fields null when not present in JSON")
        void testWrapperNulls() {
            String json = "{\"intVal\":5}";
            WrapperTypes bean = ProtifyJson.fromJson(json, WrapperTypes.class);

            assertEquals(Integer.valueOf(5), bean.getIntVal());
            assertNull(bean.getLongVal());
            assertNull(bean.getDoubleVal());
            assertNull(bean.getFloatVal());
            assertNull(bean.getBoolVal());
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — BigDecimal and BigInteger
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — BigDecimal and BigInteger")
    class FromJsonBigNumbers {

        @Test
        @DisplayName("Should deserialize BigDecimal from decimal number")
        void testBigDecimalFromDecimal() {
            String json = "{\"currency\":\"USD\",\"amount\":1234.56,\"rate\":0.0725,\"transactionId\":1}";
            FinancialRecord record = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertEquals("USD", record.getCurrency());
            assertEquals(0, new BigDecimal("1234.56").compareTo(record.getAmount()));
            assertEquals(0, new BigDecimal("0.0725").compareTo(record.getRate()));
        }

        @Test
        @DisplayName("Should deserialize BigDecimal from integer number")
        void testBigDecimalFromInteger() {
            String json = "{\"currency\":\"EUR\",\"amount\":500,\"rate\":1,\"transactionId\":1}";
            FinancialRecord record = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertEquals(0, new BigDecimal("500").compareTo(record.getAmount()));
        }

        @Test
        @DisplayName("Should deserialize BigDecimal with high precision")
        void testBigDecimalHighPrecision() {
            String json = "{\"currency\":\"BTC\",\"amount\":0.00000001,\"rate\":1,\"transactionId\":1}";
            FinancialRecord record = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertNotNull(record.getAmount());
            assertTrue(record.getAmount().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("Should deserialize BigInteger from integer number")
        void testBigIntegerFromInteger() {
            String json = "{\"currency\":\"USD\",\"amount\":1,\"rate\":1,\"transactionId\":9999999999}";
            FinancialRecord record = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertEquals(new BigInteger("9999999999"), record.getTransactionId());
        }

        @Test
        @DisplayName("Should deserialize BigInteger from very large number string")
        void testBigIntegerFromLargeNumber() {
            String json = "{\"currency\":\"USD\",\"amount\":1,\"rate\":1,\"transactionId\":99999999999999999}";
            FinancialRecord record = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertEquals(new BigInteger("99999999999999999"), record.getTransactionId());
        }

        @Test
        @DisplayName("Should handle null BigDecimal and BigInteger")
        void testNullBigNumbers() {
            String json = "{\"currency\":\"USD\"}";
            FinancialRecord record = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertEquals("USD", record.getCurrency());
            assertNull(record.getAmount());
            assertNull(record.getRate());
            assertNull(record.getTransactionId());
        }

        @Test
        @DisplayName("Should handle BigDecimal/BigInteger mixed with primitives")
        void testMixedNumericTypes() {
            String json = "{\"price\":49.99,\"quantity\":1000000,\"count\":42,\"ratio\":3.14}";
            MixedNumericBean bean = ProtifyJson.fromJson(json, MixedNumericBean.class);

            assertEquals(0, new BigDecimal("49.99").compareTo(bean.getPrice()));
            assertEquals(new BigInteger("1000000"), bean.getQuantity());
            assertEquals(42, bean.getCount());
            assertEquals(3.14, bean.getRatio(), 0.001);
        }

        @Test
        @DisplayName("Should round-trip BigDecimal through toJson/fromJson")
        void testBigDecimalRoundTrip() {
            FinancialRecord original = new FinancialRecord();
            original.setCurrency("GBP");
            original.setAmount(new BigDecimal("9999.99"));
            original.setRate(new BigDecimal("0.03"));
            original.setTransactionId(new BigInteger("123456789"));

            String json = ProtifyJson.toJson(original);
            FinancialRecord restored = ProtifyJson.fromJson(json, FinancialRecord.class);

            assertEquals(original.getCurrency(), restored.getCurrency());
            assertEquals(0, original.getAmount().compareTo(restored.getAmount()));
            assertEquals(0, original.getRate().compareTo(restored.getRate()));
            assertEquals(original.getTransactionId(), restored.getTransactionId());
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — enums
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — enum deserialization")
    class FromJsonEnums {

        @Test
        @DisplayName("Should deserialize enum by exact name")
        void testEnumExactMatch() {
            String json = "{\"label\":\"Bug\",\"severity\":\"HIGH\"}";
            EnumBean bean = ProtifyJson.fromJson(json, EnumBean.class);

            assertEquals("Bug", bean.getLabel());
            assertEquals(Severity.HIGH, bean.getSeverity());
        }

        @Test
        @DisplayName("Should deserialize enum case-insensitively (lowercase)")
        void testEnumLowercase() {
            String json = "{\"label\":\"Bug\",\"severity\":\"high\"}";
            EnumBean bean = ProtifyJson.fromJson(json, EnumBean.class);

            assertEquals(Severity.HIGH, bean.getSeverity());
        }

        @Test
        @DisplayName("Should deserialize enum case-insensitively (mixed case)")
        void testEnumMixedCase() {
            String json = "{\"label\":\"Bug\",\"severity\":\"High\"}";
            EnumBean bean = ProtifyJson.fromJson(json, EnumBean.class);

            assertEquals(Severity.HIGH, bean.getSeverity());
        }

        @Test
        @DisplayName("Should deserialize enum case-insensitively (all variations)")
        void testEnumAllCaseVariations() {
            for (String value : new String[]{"CRITICAL", "critical", "Critical", "cRiTiCaL"}) {
                String json = "{\"label\":\"Test\",\"severity\":\"" + value + "\"}";
                EnumBean bean = ProtifyJson.fromJson(json, EnumBean.class);

                assertEquals(Severity.CRITICAL, bean.getSeverity(),
                        "Failed for enum value: " + value);
            }
        }

        @Test
        @DisplayName("Should throw on invalid enum value with a descriptive message listing valid constants")
        void testInvalidEnumMessage() {
            String json = "{\"label\":\"Bug\",\"severity\":\"UNKNOWN\"}";

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ProtifyJson.fromJson(json, EnumBean.class));
            String msg = ex.getMessage();
            assertTrue(msg.contains("UNKNOWN"), "Should include the bad value");
            assertTrue(msg.contains("Severity"), "Should include the enum type");
            assertTrue(msg.contains("LOW"), "Should list valid constants");
            assertTrue(msg.contains("HIGH"), "Should list valid constants");
            assertTrue(msg.contains("CRITICAL"), "Should list valid constants");
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — nested objects
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — nested objects")
    class FromJsonNested {

        @Test
        @DisplayName("Should deserialize nested object")
        void testNestedObject() {
            String json = "{\"name\":\"Eve\",\"address\":{\"street\":\"123 Main St\",\"city\":\"Springfield\",\"zip\":62701}}";
            Person person = ProtifyJson.fromJson(json, Person.class);

            assertEquals("Eve", person.getName());
            assertNotNull(person.getAddress());
            assertEquals("123 Main St", person.getAddress().getStreet());
            assertEquals("Springfield", person.getAddress().getCity());
            assertEquals(62701, person.getAddress().getZip());
        }

        @Test
        @DisplayName("Should handle null nested object")
        void testNullNestedObject() {
            String json = "{\"name\":\"Frank\",\"address\":null}";
            Person person = ProtifyJson.fromJson(json, Person.class);

            assertEquals("Frank", person.getName());
            assertNull(person.getAddress());
        }

        @Test
        @DisplayName("Should deserialize three levels deep")
        void testDeeplyNested() {
            String json = "{\"label\":\"Report\",\"data\":{\"name\":\"HQ\","
                    + "\"addresses\":[{\"street\":\"1st Ave\",\"city\":\"NYC\",\"zip\":10001}]}}";
            DeeplyNested dn = ProtifyJson.fromJson(json, DeeplyNested.class);

            assertEquals("Report", dn.getLabel());
            assertNotNull(dn.getData());
            assertEquals("HQ", dn.getData().getName());
            assertEquals(1, dn.getData().getAddresses().size());
            assertEquals("NYC", dn.getData().getAddresses().get(0).getCity());
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — collections
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — lists and maps")
    class FromJsonCollections {

        @Test
        @DisplayName("Should deserialize List<String>")
        void testStringList() {
            String json = "{\"title\":\"Post\",\"tags\":[\"java\",\"ai\",\"sdk\"]}";
            WithList bean = ProtifyJson.fromJson(json, WithList.class);

            assertEquals("Post", bean.getTitle());
            assertEquals(List.of("java", "ai", "sdk"), bean.getTags());
        }

        @Test
        @DisplayName("Should deserialize empty list")
        void testEmptyList() {
            String json = "{\"title\":\"Empty\",\"tags\":[]}";
            WithList bean = ProtifyJson.fromJson(json, WithList.class);

            assertNotNull(bean.getTags());
            assertTrue(bean.getTags().isEmpty());
        }

        @Test
        @DisplayName("Should deserialize List<Object> with typed elements")
        void testObjectList() {
            String json = "{\"name\":\"Corp\",\"addresses\":["
                    + "{\"street\":\"A St\",\"city\":\"CityA\",\"zip\":1},"
                    + "{\"street\":\"B St\",\"city\":\"CityB\",\"zip\":2}"
                    + "]}";
            WithObjectList bean = ProtifyJson.fromJson(json, WithObjectList.class);

            assertEquals(2, bean.getAddresses().size());
            assertEquals("A St", bean.getAddresses().get(0).getStreet());
            assertEquals("CityB", bean.getAddresses().get(1).getCity());
        }

        @Test
        @DisplayName("Should deserialize Map<String, Integer>")
        void testIntegerMap() {
            String json = "{\"label\":\"Scores\",\"scores\":{\"math\":95,\"english\":88}}";
            WithMap bean = ProtifyJson.fromJson(json, WithMap.class);

            assertEquals("Scores", bean.getLabel());
            assertEquals(2, bean.getScores().size());
            assertEquals(Integer.valueOf(95), bean.getScores().get("math"));
            assertEquals(Integer.valueOf(88), bean.getScores().get("english"));
        }

        @Test
        @DisplayName("Should deserialize Map<String, Object> with nested objects")
        void testObjectMap() {
            String json = "{\"offices\":{\"nyc\":{\"street\":\"Broadway\",\"city\":\"New York\",\"zip\":10001},"
                    + "\"sf\":{\"street\":\"Market\",\"city\":\"San Francisco\",\"zip\":94105}}}";
            WithObjectMap bean = ProtifyJson.fromJson(json, WithObjectMap.class);

            assertEquals(2, bean.getOffices().size());
            assertEquals("Broadway", bean.getOffices().get("nyc").getStreet());
            assertEquals(94105, bean.getOffices().get("sf").getZip());
        }

        @Test
        @DisplayName("Should deserialize empty map")
        void testEmptyMap() {
            String json = "{\"label\":\"Empty\",\"scores\":{}}";
            WithMap bean = ProtifyJson.fromJson(json, WithMap.class);

            assertNotNull(bean.getScores());
            assertTrue(bean.getScores().isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson — arrays
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — array fields")
    class FromJsonArrays {

        @Test
        @DisplayName("Should deserialize String[]")
        void testStringArray() {
            String json = "{\"title\":\"Post\",\"tags\":[\"java\",\"ai\",\"sdk\"]}";
            WithStringArray bean = ProtifyJson.fromJson(json, WithStringArray.class);

            assertEquals("Post", bean.getTitle());
            assertArrayEquals(new String[]{"java", "ai", "sdk"}, bean.getTags());
        }

        @Test
        @DisplayName("Should deserialize empty String[]")
        void testEmptyStringArray() {
            String json = "{\"title\":\"Empty\",\"tags\":[]}";
            WithStringArray bean = ProtifyJson.fromJson(json, WithStringArray.class);

            assertNotNull(bean.getTags());
            assertEquals(0, bean.getTags().length);
        }

        @Test
        @DisplayName("Should deserialize int[]")
        void testIntArray() {
            String json = "{\"values\":[10,20,30,40]}";
            WithIntArray bean = ProtifyJson.fromJson(json, WithIntArray.class);

            assertArrayEquals(new int[]{10, 20, 30, 40}, bean.getValues());
        }

        @Test
        @DisplayName("Should deserialize array of objects (Address[])")
        void testObjectArray() {
            String json = "{\"name\":\"Corp\",\"addresses\":["
                    + "{\"street\":\"A St\",\"city\":\"CityA\",\"zip\":1},"
                    + "{\"street\":\"B St\",\"city\":\"CityB\",\"zip\":2}"
                    + "]}";
            WithObjectArray bean = ProtifyJson.fromJson(json, WithObjectArray.class);

            assertEquals("Corp", bean.getName());
            assertEquals(2, bean.getAddresses().length);
            assertEquals("A St", bean.getAddresses()[0].getStreet());
            assertEquals("CityB", bean.getAddresses()[1].getCity());
            assertEquals(2, bean.getAddresses()[1].getZip());
        }

        @Test
        @DisplayName("Should leave array field null when not present in JSON")
        void testMissingArrayField() {
            String json = "{\"title\":\"NoTags\"}";
            WithStringArray bean = ProtifyJson.fromJson(json, WithStringArray.class);

            assertEquals("NoTags", bean.getTitle());
            assertNull(bean.getTags());
        }

        @Test
        @DisplayName("Should handle single-element array")
        void testSingleElementArray() {
            String json = "{\"values\":[99]}";
            WithIntArray bean = ProtifyJson.fromJson(json, WithIntArray.class);

            assertArrayEquals(new int[]{99}, bean.getValues());
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJsonList
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJsonList — array root deserialization")
    class FromJsonListTests {

        @Test
        @DisplayName("Should deserialize a JSON array into List<T>")
        void testBasicList() {
            String json = "[{\"name\":\"Alice\",\"age\":30},{\"name\":\"Bob\",\"age\":25}]";
            List<SimpleBean> list = ProtifyJson.fromJsonList(json, SimpleBean.class);

            assertEquals(2, list.size());
            assertEquals("Alice", list.get(0).getName());
            assertEquals(25, list.get(1).getAge());
        }

        @Test
        @DisplayName("Should deserialize empty JSON array")
        void testEmptyArray() {
            List<SimpleBean> list = ProtifyJson.fromJsonList("[]", SimpleBean.class);
            assertTrue(list.isEmpty());
        }

        @Test
        @DisplayName("Should deserialize single-element array")
        void testSingleElement() {
            String json = "[{\"name\":\"Solo\",\"age\":1}]";
            List<SimpleBean> list = ProtifyJson.fromJsonList(json, SimpleBean.class);

            assertEquals(1, list.size());
            assertEquals("Solo", list.get(0).getName());
        }

        @Test
        @DisplayName("Should throw when root is not an array")
        void testNonArrayRoot() {
            String json = "{\"name\":\"NotAnArray\"}";

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ProtifyJson.fromJsonList(json, SimpleBean.class));
            assertTrue(ex.getMessage().contains("not an array"));
        }
    }

    // ---------------------------------------------------------------
    // Tests: @ProtifyJsonProperty annotation
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — @ProtifyJsonProperty field mapping")
    class FromJsonAnnotation {

        @Test
        @DisplayName("Should map JSON keys to annotated field names")
        void testAnnotatedFields() {
            String json = "{\"full_name\":\"protify/sdk\",\"star_count\":42,\"description\":\"A Java SDK\"}";
            AnnotatedBean bean = ProtifyJson.fromJson(json, AnnotatedBean.class);

            assertEquals("protify/sdk", bean.getFullName());
            assertEquals(42, bean.getStarCount());
            assertEquals("A Java SDK", bean.getDescription());
        }

        @Test
        @DisplayName("Should leave annotated fields at defaults when JSON uses Java field names instead")
        void testAnnotationRequired() {
            String json = "{\"fullName\":\"wrong\",\"starCount\":99,\"description\":\"ok\"}";
            AnnotatedBean bean = ProtifyJson.fromJson(json, AnnotatedBean.class);

            // fullName and starCount shouldn't match — annotation maps to full_name / star_count
            assertNull(bean.getFullName());
            assertEquals(0, bean.getStarCount());
            assertEquals("ok", bean.getDescription());
        }
    }

    // ---------------------------------------------------------------
    // Tests: inheritance
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — superclass field inheritance")
    class FromJsonInheritance {

        @Test
        @DisplayName("Should populate fields from both subclass and superclass")
        void testInheritedFields() {
            String json = "{\"id\":\"abc-123\",\"value\":\"hello\"}";
            ExtendedEntity entity = ProtifyJson.fromJson(json, ExtendedEntity.class);

            assertEquals("abc-123", entity.getId());
            assertEquals("hello", entity.getValue());
        }
    }

    // ---------------------------------------------------------------
    // Tests: field-only access (no setter)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — field access without setters")
    class FromJsonFieldAccess {

        @Test
        @DisplayName("Should set fields via setAccessible when no setter exists")
        void testFieldOnly() {
            String json = "{\"secret\":\"s3cret\",\"count\":7}";
            FieldOnlyBean bean = ProtifyJson.fromJson(json, FieldOnlyBean.class);

            assertEquals("s3cret", bean.getSecret());
            assertEquals(7, bean.getCount());
        }
    }

    // ---------------------------------------------------------------
    // Tests: error handling
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — error handling")
    class FromJsonErrors {

        @Test
        @DisplayName("Should throw on invalid JSON")
        void testInvalidJson() {
            assertThrows(RuntimeException.class,
                    () -> ProtifyJson.fromJson("not json", SimpleBean.class));
        }

        @Test
        @DisplayName("Should mention missing no-arg constructor in error message")
        void testNoDefaultConstructorMessage() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ProtifyJson.fromJson("{\"value\":1}", NoDefaultConstructor.class));
            String msg = ex.getMessage();
            assertTrue(msg.contains("NoDefaultConstructor"), "Should name the class");
            assertTrue(msg.contains("no-arg constructor"), "Should explain the cause");
        }

        @Test
        @DisplayName("Should include field name and type in conversion error message")
        void testFieldConversionErrorMessage() {
            // severity field expects an enum, but JSON has a value that doesn't match any constant
            String json = "{\"label\":\"Bug\",\"severity\":\"NOT_REAL\"}";

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ProtifyJson.fromJson(json, EnumBean.class));
            String msg = ex.getMessage();
            assertTrue(msg.contains("severity"), "Should name the failing field");
            assertTrue(msg.contains("EnumBean"), "Should name the target class");
            assertTrue(msg.contains("Severity"), "Should name the expected type");
            assertTrue(msg.contains("NOT_REAL"), "Should include the bad JSON value");
        }

        @Test
        @DisplayName("Should include field name when nested object deserialization fails")
        void testNestedFieldErrorMessage() {
            // address.zip expects an int, but JSON has a non-numeric string
            String json = "{\"name\":\"Test\",\"address\":{\"street\":\"Main\",\"city\":\"X\",\"zip\":\"not_a_number\"}}";

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ProtifyJson.fromJson(json, Person.class));
            String msg = ex.getMessage();
            assertTrue(msg.contains("zip"), "Should name the failing nested field");
            assertTrue(msg.contains("not_a_number"), "Should include the bad JSON value");
        }

        @Test
        @DisplayName("Should truncate very long JSON values in error messages")
        void testLongValueTruncation() {
            String longValue = "x".repeat(200);
            String json = "{\"label\":\"Bug\",\"severity\":\"" + longValue + "\"}";

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ProtifyJson.fromJson(json, EnumBean.class));
            String msg = ex.getMessage();
            assertTrue(msg.contains("truncated"), "Should indicate truncation for long values");
            assertFalse(msg.contains(longValue), "Should not include the full 200-char value");
        }
    }

    public static class NoDefaultConstructor {
        private final int value;
        public NoDefaultConstructor(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    // ---------------------------------------------------------------
    // Tests: toJson serialization (existing functionality)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("toJson — serialization")
    class ToJsonTests {

        @Test
        @DisplayName("Should serialize null to \"null\"")
        void testNullSerialization() {
            assertEquals("null", ProtifyJson.toJson(null));
        }

        @Test
        @DisplayName("Should serialize simple bean to JSON")
        void testSimpleBeanSerialization() {
            SimpleBean bean = new SimpleBean();
            bean.setName("Test");
            bean.setAge(25);

            String json = ProtifyJson.toJson(bean);
            assertTrue(json.contains("\"name\":\"Test\""));
            assertTrue(json.contains("\"age\":25"));
        }

        @Test
        @DisplayName("Should serialize enum values as strings")
        void testEnumSerialization() {
            EnumBean bean = new EnumBean();
            bean.setLabel("Issue");
            bean.setSeverity(Severity.CRITICAL);

            String json = ProtifyJson.toJson(bean);
            assertTrue(json.contains("\"severity\":\"CRITICAL\""));
        }
    }

    // ---------------------------------------------------------------
    // Tests: round-trip (toJson → fromJson)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Round-trip: toJson → fromJson")
    class RoundTrip {

        @Test
        @DisplayName("Should survive a round-trip for a simple bean")
        void testSimpleRoundTrip() {
            SimpleBean original = new SimpleBean();
            original.setName("RoundTrip");
            original.setAge(99);

            String json = ProtifyJson.toJson(original);
            SimpleBean restored = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals(original.getName(), restored.getName());
            assertEquals(original.getAge(), restored.getAge());
        }

        @Test
        @DisplayName("Should survive a round-trip for nested objects")
        void testNestedRoundTrip() {
            Address addr = new Address();
            addr.setStreet("100 Loop");
            addr.setCity("Austin");
            addr.setZip(73301);

            Person original = new Person();
            original.setName("Trip");
            original.setAddress(addr);

            String json = ProtifyJson.toJson(original);
            Person restored = ProtifyJson.fromJson(json, Person.class);

            assertEquals("Trip", restored.getName());
            assertEquals("100 Loop", restored.getAddress().getStreet());
            assertEquals(73301, restored.getAddress().getZip());
        }
    }

    // ---------------------------------------------------------------
    // Tests: escapeJson
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("escapeJson — special characters")
    class EscapeJsonTests {

        @Test
        @DisplayName("Should escape quotes, backslashes, and control characters")
        void testEscaping() {
            assertEquals("hello \\\"world\\\"", ProtifyJson.escapeJson("hello \"world\""));
            assertEquals("back\\\\slash", ProtifyJson.escapeJson("back\\slash"));
            assertEquals("line\\nbreak", ProtifyJson.escapeJson("line\nbreak"));
            assertEquals("tab\\there", ProtifyJson.escapeJson("tab\there"));
        }

        @Test
        @DisplayName("Should return empty string for null input")
        void testNullEscape() {
            assertEquals("", ProtifyJson.escapeJson(null));
        }
    }

    // ---------------------------------------------------------------
    // Tests: edge cases
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty JSON object")
        void testEmptyObject() {
            SimpleBean bean = ProtifyJson.fromJson("{}", SimpleBean.class);

            assertNull(bean.getName());
            assertEquals(0, bean.getAge());
        }

        @Test
        @DisplayName("Should handle JSON with unicode escape sequences")
        void testUnicodeEscapes() {
            String json = "{\"name\":\"caf\\u00e9\",\"age\":1}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("caf\u00e9", bean.getName());
        }

        @Test
        @DisplayName("Should handle strings containing special JSON characters")
        void testSpecialCharactersInValues() {
            String json = "{\"name\":\"line1\\nline2\\ttab\",\"age\":0}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("line1\nline2\ttab", bean.getName());
        }

        @Test
        @DisplayName("Should handle negative numbers")
        void testNegativeNumbers() {
            String json = "{\"name\":\"Negative\",\"age\":-5}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals(-5, bean.getAge());
        }

        @Test
        @DisplayName("Should handle decimal number coerced to int")
        void testDecimalToInt() {
            String json = "{\"name\":\"Decimal\",\"age\":30.0}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals(30, bean.getAge());
        }

        @Test
        @DisplayName("Should not NPE when JSON null maps to a primitive int field")
        void testNullToPrimitiveInt() {
            String json = "{\"name\":\"Test\",\"age\":null}";
            SimpleBean bean = ProtifyJson.fromJson(json, SimpleBean.class);

            assertEquals("Test", bean.getName());
            assertEquals(0, bean.getAge());
        }

        @Test
        @DisplayName("Should not NPE when JSON null maps to any primitive type")
        void testNullToAllPrimitives() {
            String json = "{\"intVal\":null,\"longVal\":null,\"doubleVal\":null,"
                    + "\"floatVal\":null,\"boolVal\":null,\"shortVal\":null,\"byteVal\":null}";
            AllPrimitives bean = ProtifyJson.fromJson(json, AllPrimitives.class);

            assertEquals(0, bean.getIntVal());
            assertEquals(0L, bean.getLongVal());
            assertEquals(0.0, bean.getDoubleVal(), 0.0);
            assertEquals(0.0f, bean.getFloatVal(), 0.0f);
            assertFalse(bean.isBoolVal());
            assertEquals((short) 0, bean.getShortVal());
            assertEquals((byte) 0, bean.getByteVal());
        }

        @Test
        @DisplayName("Should still set wrapper types to null when JSON value is null")
        void testNullToWrapperTypes() {
            String json = "{\"intVal\":null,\"longVal\":null,\"doubleVal\":null,"
                    + "\"floatVal\":null,\"boolVal\":null}";
            WrapperTypes bean = ProtifyJson.fromJson(json, WrapperTypes.class);

            assertNull(bean.getIntVal());
            assertNull(bean.getLongVal());
            assertNull(bean.getDoubleVal());
            assertNull(bean.getFloatVal());
            assertNull(bean.getBoolVal());
        }

        @Test
        @DisplayName("Should handle mix of null and non-null values with primitives")
        void testMixedNullAndPrimitives() {
            String json = "{\"intVal\":42,\"longVal\":null,\"doubleVal\":3.14,"
                    + "\"floatVal\":null,\"boolVal\":true,\"shortVal\":null,\"byteVal\":7}";
            AllPrimitives bean = ProtifyJson.fromJson(json, AllPrimitives.class);

            assertEquals(42, bean.getIntVal());
            assertEquals(0L, bean.getLongVal());
            assertEquals(3.14, bean.getDoubleVal(), 0.001);
            assertEquals(0.0f, bean.getFloatVal(), 0.0f);
            assertTrue(bean.isBoolVal());
            assertEquals((short) 0, bean.getShortVal());
            assertEquals((byte) 7, bean.getByteVal());
        }
    }

    // ---------------------------------------------------------------
    // Tests: extractJson — LLM output cleaning
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("extractJson — markdown and prose stripping")
    class ExtractJsonTests {

        @Test
        @DisplayName("Should return clean JSON object as-is")
        void testCleanObject() {
            assertEquals("{\"a\":1}", ProtifyJson.extractJson("{\"a\":1}"));
        }

        @Test
        @DisplayName("Should return clean JSON array as-is")
        void testCleanArray() {
            assertEquals("[1,2,3]", ProtifyJson.extractJson("[1,2,3]"));
        }

        @Test
        @DisplayName("Should trim whitespace around clean JSON")
        void testWhitespaceTrimming() {
            assertEquals("{\"a\":1}", ProtifyJson.extractJson("  \n  {\"a\":1}  \n  "));
        }

        @Test
        @DisplayName("Should extract JSON from ```json code fence")
        void testJsonCodeFence() {
            String input = "Here's the JSON:\n\n```json\n{\"title\":\"Inception\",\"rating\":9}\n```\n";
            assertEquals("{\"title\":\"Inception\",\"rating\":9}", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should extract JSON from ``` code fence without language tag")
        void testPlainCodeFence() {
            String input = "Result:\n```\n{\"name\":\"test\"}\n```";
            assertEquals("{\"name\":\"test\"}", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should extract JSON array from code fence")
        void testArrayInCodeFence() {
            String input = "Here are the results:\n\n```json\n[{\"id\":1},{\"id\":2}]\n```\nDone.";
            assertEquals("[{\"id\":1},{\"id\":2}]", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should extract JSON from surrounding prose (no code fence)")
        void testProseBeforeAndAfter() {
            String input = "Sure, here's the data: {\"name\":\"Alice\",\"age\":30} Hope that helps!";
            assertEquals("{\"name\":\"Alice\",\"age\":30}", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should extract JSON array from surrounding prose")
        void testArrayInProse() {
            String input = "The results are: [{\"id\":1},{\"id\":2}] as requested.";
            assertEquals("[{\"id\":1},{\"id\":2}]", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should handle nested braces in prose extraction")
        void testNestedBracesInProse() {
            String input = "Here: {\"outer\":{\"inner\":{\"deep\":true}}} done";
            assertEquals("{\"outer\":{\"inner\":{\"deep\":true}}}", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should not be confused by braces inside strings")
        void testBracesInsideStrings() {
            String input = "Result: {\"msg\":\"use {curly} braces\"} end";
            assertEquals("{\"msg\":\"use {curly} braces\"}", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should handle escaped quotes inside strings during extraction")
        void testEscapedQuotesInProse() {
            String input = "Output: {\"msg\":\"she said \\\"hello\\\"\"} done";
            assertEquals("{\"msg\":\"she said \\\"hello\\\"\"}", ProtifyJson.extractJson(input));
        }

        @Test
        @DisplayName("Should handle multiline JSON in code fence")
        void testMultilineCodeFence() {
            String input = "```json\n{\n  \"name\": \"Test\",\n  \"value\": 42\n}\n```";
            String extracted = ProtifyJson.extractJson(input);
            assertTrue(extracted.contains("\"name\""));
            assertTrue(extracted.contains("\"value\""));
        }

        @Test
        @DisplayName("Should handle null and empty input gracefully")
        void testNullAndEmpty() {
            assertNull(ProtifyJson.extractJson(null));
            assertEquals("", ProtifyJson.extractJson(""));
        }
    }

    // ---------------------------------------------------------------
    // Tests: fromJson with LLM-wrapped output (end-to-end)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("fromJson — end-to-end with LLM-wrapped output")
    class FromJsonLlmOutput {

        @Test
        @DisplayName("Should deserialize JSON wrapped in ```json code fence")
        void testCodeFenceEndToEnd() {
            String llmOutput = "Here's the JSON:\n\n```json\n{\"name\":\"Alice\",\"age\":30}\n```\n";
            SimpleBean bean = ProtifyJson.fromJson(llmOutput, SimpleBean.class);

            assertEquals("Alice", bean.getName());
            assertEquals(30, bean.getAge());
        }

        @Test
        @DisplayName("Should deserialize JSON with prose before and after")
        void testProseEndToEnd() {
            String llmOutput = "Sure! Here is the requested data: {\"name\":\"Bob\",\"age\":25} Let me know if you need anything else.";
            SimpleBean bean = ProtifyJson.fromJson(llmOutput, SimpleBean.class);

            assertEquals("Bob", bean.getName());
            assertEquals(25, bean.getAge());
        }

        @Test
        @DisplayName("Should deserialize JSON array from code fence via fromJsonList")
        void testCodeFenceListEndToEnd() {
            String llmOutput = "```json\n[{\"name\":\"A\",\"age\":1},{\"name\":\"B\",\"age\":2}]\n```";
            List<SimpleBean> list = ProtifyJson.fromJsonList(llmOutput, SimpleBean.class);

            assertEquals(2, list.size());
            assertEquals("A", list.get(0).getName());
            assertEquals(2, list.get(1).getAge());
        }

        @Test
        @DisplayName("Should deserialize nested objects from LLM prose")
        void testNestedInProse() {
            String llmOutput = "The person is: {\"name\":\"Eve\",\"address\":{\"street\":\"Main St\",\"city\":\"NYC\",\"zip\":10001}} as requested.";
            Person person = ProtifyJson.fromJson(llmOutput, Person.class);

            assertEquals("Eve", person.getName());
            assertEquals("NYC", person.getAddress().getCity());
        }
    }
}
