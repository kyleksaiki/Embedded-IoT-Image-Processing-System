package server;

/*------------------------------------------------------------------------------
  Repository: McqItemRepository
  Purpose:
    - Extends Spring Data JPA's JpaRepository to provide CRUD access
      to McqItem entities.
    - Inherits standard methods such as save(), findById(), findAll(),
      delete(), etc.

  Notes:
    - Entity type: McqItem
    - Primary key type: Long
------------------------------------------------------------------------------*/
public interface McqItemRepository
    extends org.springframework.data.jpa.repository.JpaRepository<
    server.McqItem, java.lang.Long> {
    
}
