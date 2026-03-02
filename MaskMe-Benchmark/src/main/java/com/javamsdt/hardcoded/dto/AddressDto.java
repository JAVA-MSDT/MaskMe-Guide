package com.javamsdt.hardcoded.dto;

public record AddressDto(
    Long id,
    String street,
    String city,
    String zipCode
) {
    
    public AddressDto mask(String maskInput) {
        return new AddressDto(
            id,
            street,
            maskInput != null && maskInput.equals("maskMe") ? "***" : city,
            maskInput != null && maskInput.equals("maskMe") ? "[ZIP_MASKED]" : zipCode
        );
    }
}
