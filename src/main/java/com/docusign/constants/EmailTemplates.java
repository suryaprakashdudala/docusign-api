package com.docusign.constants;

public class EmailTemplates {

    // OTP EMAIL
    public static final String OTP_SUBJECT = "Password Reset OTP";

    public static final String OTP_BODY_TEMPLATE = """
        Hello,

        Your One-Time Password (OTP) for password reset is: %s

        This OTP is valid for the next 5 minutes.
        Please do not share this code with anyone for security reasons.

        Thank you,
        DocuSign System
        """;


    // USER CREATION EMAIL
    public static final String USER_CREATION_SUBJECT = "Welcome to Audit System";

    public static final String USER_CREATION_BODY_TEMPLATE = """
        Hello %s,

        Your account has been successfully created in the Audit System.

        Username: %s
        Temporary Password: %s

        Please log in and change your password immediately for security reasons.

        Thank you,
        DocuSign System
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
        DocuSign System
        """;
}
