package com.docusign.constants;

public class EmailTemplates {
	
    private EmailTemplates() {
        // Private constructor to prevent instantiation
    }

    // OTP EMAIL
    public static final String OTP_SUBJECT = "Password Reset OTP";

    public static final String OTP_BODY_TEMPLATE = """
        Hello,

        Your One-Time Password (OTP) for password reset is: %s

        This OTP is valid for the next 5 minutes.
        Please do not share this code with anyone for security reasons.

        Thank you,
        Document Signing Platform
        """;


    // USER CREATION EMAIL
    public static final String USER_CREATION_SUBJECT = "Welcome to Audit System";

    public static final String USER_CREATION_BODY_TEMPLATE = """
        Hello %s,

        Your account has been successfully created.

        Username: %s
        Temporary Password: %s

        Please log in and change your password.

        Thank you,
        Document Signing Platform
        """;

    // DOCUMENT COMPLETION EMAIL
    public static final String DOCUMENT_COMPLETION_SUBJECT = "Action Required: Complete Document";

    public static final String DOCUMENT_COMPLETION_BODY_TEMPLATE = """
        Hello %s,

        You have been assigned to complete a document: %s

        Please click the link below to access and complete the document:
        %s

        This link is unique to you and will expire in 7 days.

        Thank you,
        Document Signing Platform
        """;

    // FINAL DOCUMENT EMAIL
    public static final String FINAL_DOCUMENT_SUBJECT = "Document Completed: %s";

    public static final String FINAL_DOCUMENT_BODY_TEMPLATE = """
        Hello %s,

        The document '%s' has been signed by all parties.

        View final document:
        %s

        Thanks,
        Document Signing Platform
        """;
}
