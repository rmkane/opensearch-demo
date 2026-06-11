/**
 * Hand-written DTO mappers — a small MapStruct-style alternative for this demo.
 * <p>
 * <b>Layering</b>
 *
 * <pre>
 *   HTTP JSON  &lt;-&gt;  ProductResponse (dto)     &lt;-- OutboundMapper in controller
 *   ProductDocument (persistence)               &lt;-- service / repository
 *   Product (opensearch-model)                  &lt;-- optional shared domain type
 * </pre>
 *
 * Controllers map <em>out</em> via {@link OutboundMapper} implementations; they
 * should not return persistence types. Request bodies may still use internal
 * types until a {@code ProductRequest} DTO and inbound {@link Mapper} are
 * added.
 * <p>
 * <b>Which base to extend</b>
 * <ul>
 * <li>{@link AbstractOutboundMapperImpl} — response only (implement
 * {@code toDto})</li>
 * <li>{@link AbstractMapperImpl} — request + response (implement {@code toDto}
 * and {@code toEntity})</li>
 * </ul>
 *
 * <b>Conventions</b>
 * <ul>
 * <li>One {@code XxxMapper} per DTO pair, {@code @Component} in
 * {@code com.acme.opensearchdemo.mapper}</li>
 * <li>DTOs live in {@code com.acme.opensearchdemo.dto}</li>
 * <li>Keep mapping logic explicit (builder or constructor); avoid
 * reflection</li>
 * <li>Register mappers in controller constructors; unit tests may use
 * {@code new XxxMapper()}</li>
 * </ul>
 *
 * @see com.acme.opensearchdemo.mapper.ProductResponseMapper
 * @see <a href="docs/mapping.md">docs/mapping.md</a>
 */
package com.acme.common.mapper;
