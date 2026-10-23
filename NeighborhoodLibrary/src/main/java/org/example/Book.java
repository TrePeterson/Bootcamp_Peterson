package org.example;

public class Book {

    //1. Properties
    private final int id;
    private String isbn;
    private String title;
    private boolean isCheckedOut;
    private String checkedOutTo;

    //2. Constructor: new books start available.
    public Book(int id, String isbn, String title) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.isCheckedOut = false;
        this.checkedOutTo = "";
    }

    //3. Getters and setters
    public int getId() {
        return this.id;
    }

    public String getIsbn() {
        return this.isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCheckedOut() {
        return this.isCheckedOut;
    }

    public String getCheckedOutTo() {
        return this.checkedOutTo;
    }

    //4. Custom methods change the status and borrower together.
    public void checkOut(String name) {
        if (this.isCheckedOut || name == null || name.trim().isEmpty()) {
            return;
        }

        this.checkedOutTo = name.trim();
        this.isCheckedOut = true;
    }

    public void checkIn() {
        this.checkedOutTo = "";
        this.isCheckedOut = false;
    }
}