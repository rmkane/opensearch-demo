# Object mapping (DTO layer)

This project uses **hand-written mappers** in `com.acme.common.mapper` instead of MapStruct for now. The API mirrors MapStruct’s common patterns (`toDto`, `toEntity`, list helpers) without an annotation processor.

## Why not MapStruct yet?

- Small number of DTOs and trivial field copies
- No generated sources or IDE plugin required
- Easy to step through in the debugger
- Clear migration path: swap a mapper to `@Mapper` MapStruct later while keeping the same method names

When mappers grow (nested objects, many fields, multiple targets), add MapStruct to the parent POM and migrate one mapper at a time.

## Type layers

| Type | Module | Role |
| ---- | ------ | ---- |
| `Product` | `opensearch-model` | Domain record and Spring Data OpenSearch entity (`@Document`, `@Field`) |
| `ProductResponse` | `opensearch-demo-app` | HTTP **response** DTO — what clients see on GET/POST/PUT/PATCH |

```
Client JSON
    ↕  (Jackson)
ProductResponse  ←—— OutboundMapper.toDto ——  Product
    ↑                                              ↑
 Controller                                   Service / Repository
                                                   ↕
                                              OpenSearch index
```

**Today:** all product endpoints return `ProductResponse`. Writes still accept `Product` in the JSON body; a future `ProductRequest` + inbound `Mapper` would complete the boundary.

## Mapper types

### Response-only (most controllers)

```java
@Component
public class ProductResponseMapper
    extends AbstractOutboundMapperImpl<Product, ProductResponse> {

  @Override
  public ProductResponse toDto(Product entity) {
    return ProductResponse.builder()
        .id(entity.id())
        // ...
        .build();
  }
}
```

Use in the controller:

```java
return ResponseEntity.ok(productResponseMapper.toDto(service.save(document)));
return ResponseEntity.ok(productResponseMapper.toDtoList(service.findAll()));
```

### Bidirectional (request DTOs)

When you add `ProductRequest`, extend `AbstractMapperImpl` and implement both `toDto` and `toEntity`:

```java
@Component
public class ProductMapper extends AbstractMapperImpl<Product, ProductRequest> {

  @Override
  public ProductRequest toDto(Product entity) { ... }

  @Override
  public Product toEntity(ProductRequest dto) { ... }
}
```

## List helpers

`OutboundMapper` and `Mapper` provide `toDtoList` / `toEntityList` defaults that:

- return `List.of()` for null or empty input
- skip null elements in the source list

No need to reimplement these in subclasses.

## Testing

- **Unit:** construct `new ProductResponseMapper()` in controller tests (no Spring context)
- **Mapper test:** assert field mapping in `ProductResponseMapperTest`
- **Integration:** JSON snapshots under `target/` exercise the full stack

## OpenSearch mapping vs DTO mapping

Do not confuse these:

| Term | Meaning in this repo |
| ---- | -------------------- |
| **Index mapping** | OpenSearch field types (`keyword`, `text`, …) in `Product` / `ProductIndexOperations` |
| **DTO mapping** | `Product` → `ProductResponse` in `ProductResponseMapper` |

Index settings live in `opensearch-model/src/main/resources/opensearch/products/settings.json`.

## Checklist for a new resource

1. Add `{Resource}Response` (and `{Resource}Request` if inbound shape differs)
2. Add `{Resource}ResponseMapper extends AbstractOutboundMapperImpl<…>`
3. Controller returns DTOs only; inject mapper via constructor
4. Add mapper unit test for non-obvious transforms
