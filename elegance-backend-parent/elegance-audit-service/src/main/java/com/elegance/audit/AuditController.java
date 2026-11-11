package com.elegance.audit;

import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
  private final EventRepository repo;

  public AuditController(EventRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public Page<RecordedEvent> events(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST);
    return repo.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
  }
}
