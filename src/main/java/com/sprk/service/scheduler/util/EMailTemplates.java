package com.sprk.service.scheduler.util;

import com.sprk.commons.dto.amqp.EmailTemplateDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EMailTemplates {

    public EmailTemplateDTO getCertificationOnHoldTemplate(
            String candidateEmail,
            String candidateName,
            String courseGroupName,
            String supposeToBeReleased
    ) {
        String subject = "Urgent: Action Required Regarding Your Certificate";
        String messageBody = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "   <head>\n" +
                "      <meta charset=\"UTF-8\" />\n" +
                "      <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "      <title>Certificate Verification Required</title>\n" +
                "      <style>\n" +
                "         /* Styles */\n" +
                "         body {\n" +
                "         color: #000000;\n" +
                "         margin: 0;\n" +
                "         padding: 0;\n" +
                "         font-family: Arial, sans-serif;\n" +
                "         }\n" +
                "         table {\n" +
                "         border-collapse: collapse;\n" +
                "         margin: auto;\n" +
                "         }\n" +
                "         .mainTable {\n" +
                "         border: 3px solid #06375b;\n" +
                "         width: 70%;\n" +
                "         }\n" +
                "         .letterTable {\n" +
                "         width: 90%;\n" +
                "         }\n" +
                "         td {\n" +
                "         padding: 10px;\n" +
                "         text-align: left;\n" +
                "         vertical-align: top;\n" +
                "         }\n" +
                "         .letterHeading {\n" +
                "         font-size: 24px;\n" +
                "         /* font-weight: bold; */\n" +
                "         text-align: center;\n" +
                "         margin-bottom: 20px;\n" +
                "         }\n" +
                "         .boldText {\n" +
                "         font-weight: bold;\n" +
                "         }\n" +
                "         .topHead {\n" +
                "         background-color: #06375b;\n" +
                "         text-align: center;\n" +
                "         }\n" +
                "         .logo {\n" +
                "         width: 200px;\n" +
                "         height: auto;\n" +
                "         display: block;\n" +
                "         margin: 20px auto;\n" +
                "         }\n" +
                "         .socialIcon {\n" +
                "         width: 20px;\n" +
                "         height: auto;\n" +
                "         vertical-align: middle;\n" +
                "         margin-right: 5px;\n" +
                "         }\n" +
                "         .footerText {\n" +
                "         text-align: center;\n" +
                "         color: #535353;\n" +
                "         }\n" +
                "         .horizontalLine {\n" +
                "         border: 1px solid #555555;\n" +
                "         }\n" +
                "         a {\n" +
                "         color: #0074bd;\n" +
                "         }\n" +
                "         .lineHeigth {\n" +
                "         line-height: 0.5;\n" +
                "         }\n" +
                "         @media (max-width: 768px) {\n" +
                "         .mainTable {\n" +
                "         width: 100%;\n" +
                "         }\n" +
                "         .letterHeading {\n" +
                "         font-size: 14px;\n" +
                "         font-weight: normal;\n" +
                "         }\n" +
                "         p {\n" +
                "         font-size: 10px;\n" +
                "         }\n" +
                "         li {\n" +
                "         font-size: 10px;\n" +
                "         }\n" +
                "         td {\n" +
                "         padding: 0px;\n" +
                "         }\n" +
                "         .socialIcon {\n" +
                "         width: 12px;\n" +
                "         height: auto;\n" +
                "         }\n" +
                "         }\n" +
                "      </style>\n" +
                "   </head>\n" +
                "   <body>\n" +
                "      <table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" align=\"center\" class=\"mainTable\">\n" +
                "         <tr>\n" +
                "            <td class=\"topHead\">\n" +
                "               <img src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518261/sprk_logo_registered__10_rxgocl.png\"\n" +
                "                  cloudName=\"dxlzzgbfw\" class=\"logo\" />\n" +
                "            </td>\n" +
                "         </tr>\n" +
                "         <tr>\n" +
                "            <td>\n" +
                "               <p class=\"letterHeading\"> Certificate Onhold</p>\n" +
                "               <table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" class=\"letterTable\">\n" +
                "                  <tr>\n" +
                "                     <td>\n" +
                "                        <p>Dear <span class=\"boldText\">" + candidateName + ",</span></p>\n" +
                "                        <p>\n" +
                "                           This mail is to inform you that the certificate for " + courseGroupName + ", scheduled for release\n" +
                "                           on " + supposeToBeReleased + ", is currently on hold due to an issue. \n" +
                "                        </p>\n" +
                "                        <p>\n" +
                "                           To resolve this issue, we kindly request you to contact our administration team at your\n" +
                "                           earliest convenience. They will be able to provide you with the necessary information\n" +
                "                           and assistance to address the problem and help you get your certificate back on track.\n" +
                "                        </p>\n" +
                "                        <p>\n" +
                "                           Please reach out to our admin team at center for further details. \n" +
                "                        </p>\n" +
                "                        <p>Best regards,</p>\n" +
                "                        <p class=\"boldText\">SPRK Technologies</p>\n" +
                "                        <p class=\"footerlinks\">\n" +
                "                           <a href=\"https://sprktechnologies.in/home\">SPRK Website</a> |\n" +
                "                           <a href=\"http://wa.me/919082572832?text=Hello \">Contact Us</a>\n" +
                "                           |\n" +
                "                           <a href=\"mailto:sprktechnologies.kharghar@gmail.com \">Support</a>\n" +
                "                        </p>\n" +
                "                     </td>\n" +
                "                  </tr>\n" +
                "               </table>\n" +
                "               <table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" class=\"letterTable\">\n" +
                "                  <tr>\n" +
                "                     <td>\n" +
                "                        <p class=\"horizontalLine\"></p>\n" +
                "                        <p class=\"boldText\">Social Handles</p>\n" +
                "                        <p>\n" +
                "                           Follow us on\n" +
                "                           <a href=\"https://www.instagram.com/sprktech/?hl=en\"><img\n" +
                "                              src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_instagram_iee7mt.png\"\n" +
                "                              class=\"socialIcon\" />@sprktech</a>\n" +
                "                        </p>\n" +
                "                        <p>\n" +
                "                           Contact us on\n" +
                "                           <a href=\"https://in.linkedin.com/company/sprk-technologies\"><img\n" +
                "                              src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/devicon_linkedin_hfekat.png\"\n" +
                "                              class=\"socialIcon\" />SPRK Technologies</a>\n" +
                "                        </p>\n" +
                "                        <p class=\"footerText\">\n" +
                "                           Address: SPRK Technologies, Plot No-11 Opposite:, Glomax Mall,\n" +
                "                           Office:102-104,1st Floor, Royal Palace, Sector 2, Kharghar,\n" +
                "                           Navi Mumbai, Maharashtra 410210 Contact us : 090825 72832/\n" +
                "                           8425840175;\n" +
                "                        </p>\n" +
                "                     </td>\n" +
                "                  </tr>\n" +
                "               </table>\n" +
                "            </td>\n" +
                "         </tr>\n" +
                "      </table>\n" +
                "   </body>\n" +
                "</html>";

        return EmailTemplateDTO
                .builder()
                .subject(subject)
                .recipient(candidateEmail)
                .isHtml(true)
                .messageBody(messageBody)
                .build();
    }



    public EmailTemplateDTO getCertificateVerificationTemplate(
            String candidateName,
            String candidateEmail,
            String courseName,
            String studentId,
            Integer days
    ) {
        String subject = "Attention: Confirm Details for Certificate Issuance";
        String messageBody = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>Certificate Verification Required</title>\n" +
                "    <style>\n" +
                "      /* Styles */\n" +
                "      body {\n" +
                "        color: #000000;\n" +
                "        margin: 0;\n" +
                "        padding: 0;\n" +
                "        font-family: Arial, sans-serif;\n" +
                "      }\n" +
                "      table {\n" +
                "        border-collapse: collapse;\n" +
                "        margin: auto;\n" +
                "      }\n" +
                "\n" +
                "      .mainTable {\n" +
                "        border: 3px solid #06375b;\n" +
                "        width: 70%;\n" +
                "      }\n" +
                "\n" +
                "      .letterTable {\n" +
                "        width: 90%;\n" +
                "      }\n" +
                "\n" +
                "      td {\n" +
                "        padding: 10px;\n" +
                "        text-align: left;\n" +
                "        vertical-align: top;\n" +
                "      }\n" +
                "      .letterHeading {\n" +
                "        font-size: 24px;\n" +
                "        /* font-weight: bold; */\n" +
                "        text-align: center;\n" +
                "        margin-bottom: 20px;\n" +
                "      }\n" +
                "      .boldText {\n" +
                "        font-weight: bold;\n" +
                "      }\n" +
                "\n" +
                "      .topHead {\n" +
                "        background-color: #06375b;\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      .logo {\n" +
                "        width: 200px;\n" +
                "        height: auto;\n" +
                "        display: block;\n" +
                "        margin: 20px auto;\n" +
                "      }\n" +
                "      .socialIcon {\n" +
                "        width: 20px;\n" +
                "        height: auto;\n" +
                "        vertical-align: middle;\n" +
                "        margin-right: 5px;\n" +
                "      }\n" +
                "      .footerText {\n" +
                "        text-align: center;\n" +
                "        color: #535353;\n" +
                "      }\n" +
                "      .horizontalLine {\n" +
                "        border: 1px solid #555555;\n" +
                "      }\n" +
                "      a {\n" +
                "        color: #0074bd;\n" +
                "      }\n" +
                "      .lineHeigth{\n" +
                "        line-height:0.5;\n" +
                "      }\n" +
                "\n" +
                "\n" +
                "      @media (max-width: 768px) {\n" +
                "        .mainTable {\n" +
                "          width: 100%;\n" +
                "        }\n" +
                "        .letterHeading {\n" +
                "          font-size: 14px;\n" +
                "          font-weight: normal;\n" +
                "        }\n" +
                "        p {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        li {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        td {\n" +
                "          padding: 0px;\n" +
                "        }\n" +
                "\n" +
                "        .socialIcon {\n" +
                "          width: 12px;\n" +
                "          height: auto;\n" +
                "        }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <table\n" +
                "      cellspacing=\"0\"\n" +
                "      cellpadding=\"0\"\n" +
                "      border=\"0\"\n" +
                "      width=\"100%\"\n" +
                "      align=\"center\"\n" +
                "      class=\"mainTable\"\n" +
                "    >\n" +
                "      <tr>\n" +
                "        <td class=\"topHead\">\n" +
                "          <img\n" +
                "            src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518261/sprk_logo_registered__10_rxgocl.png\"\n" +
                "            cloudName=\"dxlzzgbfw\"\n" +
                "            class=\"logo\"\n" +
                "          />\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          <p class=\"letterHeading\">Certificate Details Confirmation</p>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p>Dear <span class=\"boldText\">"+candidateName+",</span></p>\n" +
                "                <p>\n" +
                "                    Your certificate for the "+courseName+" course will be ready soon. Confirm your details:\n" +
                "                </p>\n" +
                "\n" +
                "                <div class=\"lineHeigth\">\n" +
                "                    <p>Name: "+candidateName+"</p>\n" +
                "                    <p>Student ID: "+studentId+"</p>\n" +
                "                </div>\n" +
                "                <p>Review and notify any changes within " + days + " days. No amendments will be made after certificate issuance.</p>\n" +
                "                <p>\n" +
                "                    Thank you for your cooperation.\n" +
                "                </p>\n" +
                "                <p>Best regards,</p>\n" +
                "                <p class=\"boldText\">SPRK Technologies</p>\n" +
                "                <p class=\"footerlinks\">\n" +
                "                  <a href=\"https://sprktechnologies.in/home\">Website</a> |\n" +
                "                  <a href=\"http://wa.me/919082572832?text=Hello \">Contact Us</a>\n" +
                "                  |\n" +
                "                  <a href=\"mailto:sprktechnologies.kharghar@gmail.com\">Support</a>\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p class=\"horizontalLine\"></p>\n" +
                "                <p class=\"boldText\">Social Handles</p>\n" +
                "                <p>\n" +
                "                  Follow us on\n" +
                "                  <a href=\"https://www.instagram.com/sprktech/?hl=en\">\n" +
                "                    <img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_instagram_iee7mt.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />@sprktech</a>\n" +
                "                </p>\n" +
                "                <p>\n" +
                "                  Contact us on\n" +
                "                  <a href=\"https://in.linkedin.com/company/sprk-technologies\">\n" +
                "                    <img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/devicon_linkedin_hfekat.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />LinkedIn</a>\n" +
                "                </p>\n" +
                "                <p class=\"footerText\">\n" +
                "                  © 2023 SPRK Technologies. All rights reserved.\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";

        return EmailTemplateDTO
                .builder()
                .subject(subject)
                .recipient(candidateEmail)
                .isHtml(true)
                .messageBody(messageBody)
                .build();
    }




    public EmailTemplateDTO getDownloadCertificateTemplate(
            String candidateName,
            String candidateEmail,
            String courseName,
            String downloadLink
    ) {
        String subject = "Your course certificate has been issued and is available for download";
        String messageBody = "<!DOCTYPE html>\n" +
                "  <html lang=\"en\">\n" +
                "    <head>\n" +
                "      <meta charset=\"UTF-8\" />\n" +
                "      <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "      <title>Certificate Issuance</title>\n" +
                "      <style>\n" +
                "        /* Styles */\n" +
                "        body {\n" +
                "          color: #000000;\n" +
                "          margin: 0;\n" +
                "          padding: 0;\n" +
                "          font-family: Arial, sans-serif;\n" +
                "        }\n" +
                "        table {\n" +
                "          border-collapse: collapse;\n" +
                "          margin: auto;\n" +
                "        }\n" +
                "        .mainTable {\n" +
                "          border: 3px solid #06375b;\n" +
                "          width: 70%;\n" +
                "        }\n" +
                "        .letterTable {\n" +
                "          width: 90%;\n" +
                "        }\n" +
                "        td {\n" +
                "          padding: 10px;\n" +
                "          text-align: left;\n" +
                "          vertical-align: top;\n" +
                "        }\n" +
                "        .letterHeading {\n" +
                "          font-size: 24px;\n" +
                "          /* font-weight: bold; */\n" +
                "          text-align: center;\n" +
                "          margin-bottom: 20px;\n" +
                "        }\n" +
                "        .boldText {\n" +
                "          font-weight: bold;\n" +
                "        }\n" +
                "        .topHead {\n" +
                "          background-color: #06375b;\n" +
                "          text-align: center;\n" +
                "        }\n" +
                "        .logo {\n" +
                "          width: 200px;\n" +
                "          height: auto;\n" +
                "          display: block;\n" +
                "          margin: 20px auto;\n" +
                "        }\n" +
                "        .socialIcon {\n" +
                "          width: 20px;\n" +
                "          height: auto;\n" +
                "          vertical-align: middle;\n" +
                "          margin-right: 5px;\n" +
                "        }\n" +
                "        .footerText {\n" +
                "          text-align: center;\n" +
                "          color: #535353;\n" +
                "        }\n" +
                "        .horizontalLine {\n" +
                "          border: 1px solid #555555;\n" +
                "        }\n" +
                "        a {\n" +
                "          color: #0074bd;\n" +
                "        }\n" +
                "        .button {\n" +
                "          display: inline-block;\n" +
                "          padding: 10px 20px;\n" +
                "          background-color: #0074bd;\n" +
                "          color: #fff;\n" +
                "          text-decoration: none;\n" +
                "          border-radius: 4px;\n" +
                "          /* margin-left: 40px; */\n" +
                "        }\n" +
                "        @media (max-width: 768px) {\n" +
                "          .mainTable {\n" +
                "            width: 100%;\n" +
                "          }\n" +
                "          .letterHeading {\n" +
                "            font-size: 14px;\n" +
                "            font-weight: normal;\n" +
                "          }\n" +
                "          p {\n" +
                "            font-size: 10px;\n" +
                "          }\n" +
                "          li {\n" +
                "            font-size: 10px;\n" +
                "          }\n" +
                "          td {\n" +
                "            padding: 0px;\n" +
                "          }\n" +
                "          .socialIcon {\n" +
                "            width: 12px;\n" +
                "            height: auto;\n" +
                "          }\n" +
                "          .button {\n" +
                "            display: inline-block;\n" +
                "            padding: 5px 10px;\n" +
                "            background-color: #0074bd;\n" +
                "            /* margin-left: 40px; */\n" +
                "          }\n" +
                "        }\n" +
                "      </style>\n" +
                "    </head>\n" +
                "    <body>\n" +
                "      <table\n" +
                "        cellspacing=\"0\"\n" +
                "        cellpadding=\"0\"\n" +
                "        border=\"0\"\n" +
                "        width=\"100%\"\n" +
                "        align=\"center\"\n" +
                "        class=\"mainTable\"\n" +
                "      >\n" +
                "        <tr>\n" +
                "          <td class=\"topHead\">\n" +
                "            <img\n" +
                "              src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518261/sprk_logo_registered__10_rxgocl.png\"\n" +
                "              cloudName=\"dxlzzgbfw\"\n" +
                "              class=\"logo\"\n" +
                "            />\n" +
                "          </td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <td>\n" +
                "            <p class=\"letterHeading\">Certificate Issued</p>\n" +
                "            <table\n" +
                "              cellspacing=\"0\"\n" +
                "              cellpadding=\"0\"\n" +
                "              border=\"0\"\n" +
                "              width=\"100%\"\n" +
                "              class=\"letterTable\"\n" +
                "            >\n" +
                "              <tr>\n" +
                "                <td>\n" +
                "                  <p>\n" +
                "                    Dear <span class=\"boldText\">"+candidateName+",</span>\n" +
                "                  </p>\n" +
                "                  <p>Congratulations on completion of "+courseName+" course,</p>\n" +
                "                  <p> We are pleased to inform you that your certificate for  is now available for download.</p>\n" +
                "                  <div>\n" +
                "                    <p>The certificate is password-protected. The password consists of two parts:</p>\n" +
                "                    <p>1.The first 2 characters of your name in capital.</p>\n" +
                "                    <p>2.Your date of birth in the mmyyyy format.</p>\n" +
                "                    <p>  For example, if your name is  Vivaan Sharma and your date of birth is February 1, 1981, the password will be VI021981 .</p>\n" +
                "                    <p>\n" +
                "                      To download the certificate click on the button below:\n" +
                "                    </p>\n" +
                "                    <a href="+downloadLink+">\n" +
                "                      <p class=\"button\">Download</p>\n" +
                "                    </a>\n" +
                "                    <p><span style=\"font-weight: bold;\">Important :</span> Use the name and date of birth as recorded during your course enrollment. If the password does not work, please contact your institute to verify the recorded date of birth.</p>\n" +
                "                  </div>\n" +
                "                  <p>If you have any questions or need further assistance, please feel free to reach out.</p>\n" +
                "                  <p>Best regards,</p>\n" +
                "                  <p class=\"boldText\">SPRK Technologies</p>\n" +
                "                  <p class=\"footerlinks\">\n" +
                "                    <a href=\"https://sprktechnologies.in/home\">SPRK Website</a> |\n" +
                "                    <a href=\"http://wa.me/919082572832?text=Hello \">Contact Us</a>\n" +
                "                    |\n" +
                "                    <a href=\"mailto:sprktechnologies.kharghar@gmail.com \"\n" +
                "                      >Support</a\n" +
                "                    >\n" +
                "                  </p>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "            </table>\n" +
                "            <table\n" +
                "              cellspacing=\"0\"\n" +
                "              cellpadding=\"0\"\n" +
                "              border=\"0\"\n" +
                "              width=\"100%\"\n" +
                "              class=\"letterTable\"\n" +
                "            >\n" +
                "              <tr>\n" +
                "                <td>\n" +
                "                  <p class=\"horizontalLine\"></p>\n" +
                "                  <p class=\"boldText\">Social Handles</p>\n" +
                "                  <p>\n" +
                "                    Follow us on\n" +
                "                    <a href=\"https://www.instagram.com/sprktech/?hl=en\"\n" +
                "                      ><img\n" +
                "                        src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_instagram_iee5va.svg\"\n" +
                "                        alt=\"Instagram\"\n" +
                "                        class=\"socialIcon\"\n" +
                "                      />\n" +
                "                      Instagram</a\n" +
                "                    >,\n" +
                "                    <a href=\"https://www.facebook.com/sprktechnologies.in/\"\n" +
                "                      ><img\n" +
                "                        src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518051/skill-icons_facebook_qbpdbf.svg\"\n" +
                "                        alt=\"Facebook\"\n" +
                "                        class=\"socialIcon\"\n" +
                "                      />\n" +
                "                      Facebook</a\n" +
                "                    >,\n" +
                "                    <a href=\"https://twitter.com/sprk_tech?s=11&t=G9gOgO14EKRWN0f3eNVlKg\"\n" +
                "                      ><img\n" +
                "                        src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701517982/skill-icons_twitter_cuwckj.svg\"\n" +
                "                        alt=\"Twitter\"\n" +
                "                        class=\"socialIcon\"\n" +
                "                      />\n" +
                "                      Twitter</a\n" +
                "                    >,\n" +
                "                    <a href=\"https://in.linkedin.com/company/sprktechnologies\"\n" +
                "                      ><img\n" +
                "                        src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701517899/skill-icons_linkedin_cxtucg.svg\"\n" +
                "                        alt=\"LinkedIn\"\n" +
                "                        class=\"socialIcon\"\n" +
                "                      />\n" +
                "                      LinkedIn</a\n" +
                "                    >\n" +
                "                  </p>\n" +
                "                  <p class=\"footerText\">\n" +
                "                    Please do not reply to this email. Emails sent to this address will\n" +
                "                    not be answered. Copyright © 2023\n" +
                "                    <a href=\"https://sprktechnologies.in/home\">SPRK Technologies</a>\n" +
                "                  </p>\n" +
                "                </td>\n" +
                "              </tr>\n" +
                "            </table>\n" +
                "          </td>\n" +
                "        </tr>\n" +
                "      </table>\n" +
                "    </body>\n" +
                "  </html>\n";

        return EmailTemplateDTO
                .builder()
                .subject(subject)
                .recipient(candidateEmail)
                .isHtml(true)
                .messageBody(messageBody)
                .build();
    }




    public EmailTemplateDTO getEarlyReleaseCertificateTemplate(
            String candidateName,
            String candidateEmail,
            String courseName,
            String downloadLink
    ) {
        String subject = "Your course certificate has been issued ahead of schedule and is ready for download";
        String messageBody = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>Certificate Issuance</title>\n" +
                "    <style>\n" +
                "      /* Styles */\n" +
                "      body {\n" +
                "        color: #000000;\n" +
                "        margin: 0;\n" +
                "        padding: 0;\n" +
                "        font-family: Arial, sans-serif;\n" +
                "      }\n" +
                "      table {\n" +
                "        border-collapse: collapse;\n" +
                "        margin: auto;\n" +
                "      }\n" +
                "      .mainTable {\n" +
                "        border: 3px solid #06375b;\n" +
                "        width: 70%;\n" +
                "      }\n" +
                "      .letterTable {\n" +
                "        width: 90%;\n" +
                "      }\n" +
                "      td {\n" +
                "        padding: 10px;\n" +
                "        text-align: left;\n" +
                "        vertical-align: top;\n" +
                "      }\n" +
                "      .letterHeading {\n" +
                "        font-size: 24px;\n" +
                "        text-align: center;\n" +
                "        margin-bottom: 20px;\n" +
                "      }\n" +
                "      .boldText {\n" +
                "        font-weight: bold;\n" +
                "      }\n" +
                "      .topHead {\n" +
                "        background-color: #06375b;\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      .logo {\n" +
                "        width: 200px;\n" +
                "        height: auto;\n" +
                "        display: block;\n" +
                "        margin: 20px auto;\n" +
                "      }\n" +
                "      .socialIcon {\n" +
                "        width: 20px;\n" +
                "        height: auto;\n" +
                "        vertical-align: middle;\n" +
                "        margin-right: 5px;\n" +
                "      }\n" +
                "      .footerText {\n" +
                "        text-align: center;\n" +
                "        color: #535353;\n" +
                "      }\n" +
                "      .horizontalLine {\n" +
                "        border: 1px solid #555555;\n" +
                "      }\n" +
                "      a {\n" +
                "        color: #0074bd;\n" +
                "      }\n" +
                "      .button {\n" +
                "        display: inline-block;\n" +
                "        padding: 10px 20px;\n" +
                "        background-color: #0074bd;\n" +
                "        color: #fff;\n" +
                "        text-decoration: none;\n" +
                "        border-radius: 4px;\n" +
                "      }\n" +
                "      @media (max-width: 768px) {\n" +
                "        .mainTable {\n" +
                "          width: 100%;\n" +
                "        }\n" +
                "        .letterHeading {\n" +
                "          font-size: 14px;\n" +
                "          font-weight: normal;\n" +
                "        }\n" +
                "        p {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        li {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        td {\n" +
                "          padding: 0px;\n" +
                "        }\n" +
                "        .socialIcon {\n" +
                "          width: 12px;\n" +
                "          height: auto;\n" +
                "        }\n" +
                "        .button {\n" +
                "          display: inline-block;\n" +
                "          padding: 5px 10px;\n" +
                "          background-color: #0074bd;\n" +
                "        }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <table\n" +
                "      cellspacing=\"0\"\n" +
                "      cellpadding=\"0\"\n" +
                "      border=\"0\"\n" +
                "      width=\"100%\"\n" +
                "      align=\"center\"\n" +
                "      class=\"mainTable\"\n" +
                "    >\n" +
                "      <tr>\n" +
                "        <td class=\"topHead\">\n" +
                "          <img\n" +
                "            src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518261/sprk_logo_registered__10_rxgocl.png\"\n" +
                "            cloudName=\"dxlzzgbfw\"\n" +
                "            class=\"logo\"\n" +
                "          />\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          <p class=\"letterHeading\">Certificate Issued</p>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p>\n" +
                "                  Dear <span class=\"boldText\">"+candidateName+",</span>\n" +
                "                </p>\n" +
                "                <p>Congratulations on completion of "+courseName+" course,</p>\n" +
                "                <p> We are pleased to inform you that your certificate for  is now available for download.</p>\n" +
                "                <p>Due to your request, this certificate was early released.</p>\n" +
                "                <div>\n" +
                "                  <p>The certificate is password-protected. The password consists of two parts:</p>\n" +
                "                  <p>1.The first 2 characters of your name in capital.</p>\n" +
                "                  <p>2.Your date of birth in the mmyyyy format.</p>\n" +
                "                  <p>  For example, if your name is  Vivaan Sharma and your date of birth is February 1, 1981, the password will be VI021981 .</p>\n" +
                "                  <p>\n" +
                "                    To download the certificate click on the button below:\n" +
                "                  </p>\n" +
                "                  <a href="+downloadLink+">\n" +
                "                    <p class=\"button\">Download</p>\n" +
                "                  </a>\n" +
                "                  <p><span style=\"font-weight: bold;\">Important :</span> Use the name and date of birth as recorded during your course enrollment. If the password does not work, please contact your institute to verify the recorded date of birth.</p>\n" +
                "                </div>\n" +
                "                <p>If you have any questions or need further assistance, please feel free to reach out.</p>\n" +
                "                <p>Best regards,</p>\n" +
                "                <p class=\"boldText\">SPRK Technologies</p>\n" +
                "                <p class=\"footerlinks\">\n" +
                "                  <a href=\"https://sprktechnologies.in/home\">SPRK Website</a> |\n" +
                "                  <a href=\"http://wa.me/919082572832?text=Hello \">Contact Us</a>\n" +
                "                  |\n" +
                "                  <a href=\"mailto:sprktechnologies.kharghar@gmail.com \"\n" +
                "                    >Support</a\n" +
                "                  >\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p class=\"horizontalLine\"></p>\n" +
                "                <p class=\"boldText\">Social Handles</p>\n" +
                "                <p>\n" +
                "                  Follow us on\n" +
                "                  <a href=\"https://www.instagram.com/sprktech/?hl=en\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_instagram_iee7mt.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />\n" +
                "                  </a>\n" +
                "                  <a href=\"https://www.linkedin.com/company/sprktech/?originalSubdomain=in\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_linkedin_k6e5eo.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />\n" +
                "                  </a>\n" +
                "                  <a href=\"https://github.com/orgs/SPRKTechnologies/repositories\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_github_ubfqrv.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />\n" +
                "                  </a>\n" +
                "                  <a href=\"https://discord.com/invite/5PbDBY9FNz\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_discord_mw9ewv.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />\n" +
                "                  </a>\n" +
                "                  <a href=\"https://www.facebook.com/sprktechnologies/\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_facebook_a5dazu.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />\n" +
                "                  </a>\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "            <tr>\n" +
                "              <td class=\"footerText\">\n" +
                "                <p>SPRK Technologies Kharghar, Navi Mumbai</p>\n" +
                "                <p>&copy; 2024 SPRK Technologies. All rights reserved.</p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";


        return EmailTemplateDTO
                .builder()
                .subject(subject)
                .recipient(candidateEmail)
                .isHtml(true)
                .messageBody(messageBody)
                .build();
    }




    public EmailTemplateDTO getCourseBookingExpiringEmail(
            String studentName,
            String confirmationNumber,
            String expirationDate,
            String recipientEmail
    ) {
        // Define the email subject
        String subject = "Course Booking Expiring Soon";

        // Define the message body using HTML markup
        String messageBody = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>Course Booking Expiring Soon</title>\n" +
                "    <style>\n" +
                "      body {\n" +
                "        color: #000000;\n" +
                "        margin: 0;\n" +
                "        padding: 0;\n" +
                "        font-family: Arial, sans-serif;\n" +
                "      }\n" +
                "      table {\n" +
                "        border-collapse: collapse;\n" +
                "        margin: auto;\n" +
                "      }\n" +
                "      .mainTable {\n" +
                "        border: 3px solid #06375b;\n" +
                "        width: 70%;\n" +
                "      }\n" +
                "      .letterTable {\n" +
                "        width: 90%;\n" +
                "      }\n" +
                "      td {\n" +
                "        padding: 10px;\n" +
                "        text-align: left;\n" +
                "        vertical-align: top;\n" +
                "      }\n" +
                "      .letterHeading {\n" +
                "        font-size: 24px;\n" +
                "        text-align: center;\n" +
                "        margin-bottom: 20px;\n" +
                "      }\n" +
                "      .boldText {\n" +
                "        font-weight: bold;\n" +
                "      }\n" +
                "      .topHead {\n" +
                "        background-color: #06375b;\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      .logo {\n" +
                "        width: 200px;\n" +
                "        height: auto;\n" +
                "        display: block;\n" +
                "        margin: 20px auto;\n" +
                "      }\n" +
                "      .socialIcon {\n" +
                "        width: 20px;\n" +
                "        height: auto;\n" +
                "        vertical-align: middle;\n" +
                "        margin-right: 5px;\n" +
                "      }\n" +
                "      .footerText {\n" +
                "        text-align: center;\n" +
                "        color: #535353;\n" +
                "      }\n" +
                "      .horizontalLine {\n" +
                "        border: 1px solid #555555;\n" +
                "      }\n" +
                "      a {\n" +
                "        color: #0074bd;\n" +
                "      }\n" +
                "      .lineHeight {\n" +
                "        line-height: 0.5;\n" +
                "      }\n" +
                "      @media (max-width: 768px) {\n" +
                "        .mainTable {\n" +
                "          width: 100%;\n" +
                "        }\n" +
                "        .letterHeading {\n" +
                "          font-size: 14px;\n" +
                "          font-weight: normal;\n" +
                "        }\n" +
                "        p {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        li {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        td {\n" +
                "          padding: 0px;\n" +
                "        }\n" +
                "        .socialIcon {\n" +
                "          width: 12px;\n" +
                "          height: auto;\n" +
                "        }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <table\n" +
                "      cellspacing=\"0\"\n" +
                "      cellpadding=\"0\"\n" +
                "      border=\"0\"\n" +
                "      width=\"100%\"\n" +
                "      align=\"center\"\n" +
                "      class=\"mainTable\"\n" +
                "    >\n" +
                "      <tr>\n" +
                "        <td class=\"topHead\">\n" +
                "          <img\n" +
                "            src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518261/sprk_logo_registered__10_rxgocl.png\"\n" +
                "            cloudName=\"dxlzzgbfw\"\n" +
                "            class=\"logo\"\n" +
                "          />\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          <p class=\"letterHeading\">Course Booking Expiring Soon</p>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p>Dear <span class=\"boldText\">"+studentName+",</span></p>\n" +
                "                <p>\n" +
                "                    We inform you that your course booking with confirmation number "+confirmationNumber+" will expire on "+expirationDate+". Post-expiration, you won't be able to attend batches, complete exams, or receive the certificate due to incomplete requirements.\n" +
                "                </p>\n" +
                "                <p>\n" +
                "                    To avoid this, please contact our admin team promptly.\n" +
                "                </p>\n" +
                "                <p>Best regards,</p>\n" +
                "                <p class=\"boldText\">SPRK Technologies</p>\n" +
                "                <p class=\"footerlinks\">\n" +
                "                  <a href=\"https://sprktechnologies.in/home\">SPRK Website</a> |\n" +
                "                  <a href=\"http://wa.me/919082572832?text=Hello \">Contact Us</a> |\n" +
                "                  <a href=\"mailto:sprktechnologies.kharghar@gmail.com\">Support</a>\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p class=\"horizontalLine\"></p>\n" +
                "                <p class=\"boldText\">Social Handles</p>\n" +
                "                <p>\n" +
                "                  Follow us on\n" +
                "                  <a href=\"https://www.instagram.com/sprktech/?hl=en\">\n" +
                "                    <img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_instagram_iee7mt.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />@sprktech</a>\n" +
                "                </p>\n" +
                "                <p>\n" +
                "                  Contact us on\n" +
                "                  <a href=\"https://in.linkedin.com/company/sprk-technologies\">\n" +
                "                    <img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/devicon_linkedin_hfekat.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />SPRK Technologies</a>\n" +
                "                </p>\n" +
                "                <p class=\"footerText\">\n" +
                "                  Address: SPRK Technologies, Plot No-5, Sector-11,\n" +
                "                  Kharghar, Navi Mumbai-410210\n" +
                "                </p>\n" +
                "                <p class=\"footerText\">\n" +
                "                  You have received this email because you are registered on SPRK\n" +
                "                  Technologies. If you have any questions, please contact us.\n" +
                "                </p>\n" +
                "                <p class=\"footerText\">Copyright © 2024 SPRK Technologies.</p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";

        // Set the subject and body in the EmailTemplateDTO object

        return EmailTemplateDTO.builder()
                .recipient(recipientEmail)
                .subject(subject)
                .messageBody(messageBody)
                .isHtml(true)
                .build();
    }




    public EmailTemplateDTO getCourseExpiredEmail(
            String studentName,
            String bookingConfirmationNumber,
            String expirationDate,
            String recipientEmail
    ) {
        // Define the email subject
        String subject = "Course Booking Expired";

        // Define the message body using HTML markup
        String messageBody = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "    <title>Course Expired</title>\n" +
                "    <style>\n" +
                "      body {\n" +
                "        color: #000000;\n" +
                "        margin: 0;\n" +
                "        padding: 0;\n" +
                "        font-family: Arial, sans-serif;\n" +
                "      }\n" +
                "      table {\n" +
                "        border-collapse: collapse;\n" +
                "        margin: auto;\n" +
                "      }\n" +
                "      .mainTable {\n" +
                "        border: 3px solid #06375b;\n" +
                "        width: 70%;\n" +
                "      }\n" +
                "      .letterTable {\n" +
                "        width: 90%;\n" +
                "      }\n" +
                "      td {\n" +
                "        padding: 10px;\n" +
                "        text-align: left;\n" +
                "        vertical-align: top;\n" +
                "      }\n" +
                "      .letterHeading {\n" +
                "        font-size: 24px;\n" +
                "        text-align: center;\n" +
                "        margin-bottom: 20px;\n" +
                "      }\n" +
                "      .boldText {\n" +
                "        font-weight: bold;\n" +
                "      }\n" +
                "      .topHead {\n" +
                "        background-color: #06375b;\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      .logo {\n" +
                "        width: 200px;\n" +
                "        height: auto;\n" +
                "        display: block;\n" +
                "        margin: 20px auto;\n" +
                "      }\n" +
                "      .socialIcon {\n" +
                "        width: 20px;\n" +
                "        height: auto;\n" +
                "        vertical-align: middle;\n" +
                "        margin-right: 5px;\n" +
                "      }\n" +
                "      .footerText {\n" +
                "        text-align: center;\n" +
                "        color: #535353;\n" +
                "      }\n" +
                "      .horizontalLine {\n" +
                "        border: 1px solid #555555;\n" +
                "      }\n" +
                "      a {\n" +
                "        color: #0074bd;\n" +
                "      }\n" +
                "      .lineHeight {\n" +
                "        line-height: 0.5;\n" +
                "      }\n" +
                "      @media (max-width: 768px) {\n" +
                "        .mainTable {\n" +
                "          width: 100%;\n" +
                "        }\n" +
                "        .letterHeading {\n" +
                "          font-size: 14px;\n" +
                "          font-weight: normal;\n" +
                "        }\n" +
                "        p {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        li {\n" +
                "          font-size: 10px;\n" +
                "        }\n" +
                "        td {\n" +
                "          padding: 0px;\n" +
                "        }\n" +
                "        .socialIcon {\n" +
                "          width: 12px;\n" +
                "          height: auto;\n" +
                "        }\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <table\n" +
                "      cellspacing=\"0\"\n" +
                "      cellpadding=\"0\"\n" +
                "      border=\"0\"\n" +
                "      width=\"100%\"\n" +
                "      align=\"center\"\n" +
                "      class=\"mainTable\"\n" +
                "    >\n" +
                "      <tr>\n" +
                "        <td class=\"topHead\">\n" +
                "          <img\n" +
                "            src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518261/sprk_logo_registered__10_rxgocl.png\"\n" +
                "            cloudName=\"dxlzzgbfw\"\n" +
                "            class=\"logo\"\n" +
                "          />\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "      <tr>\n" +
                "        <td>\n" +
                "          <p class=\"letterHeading\">Course Expired</p>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p>Dear <span class=\"boldText\">"+studentName+",</span></p>\n" +
                "                <p>\n" +
                "                    We regret to inform you that your course booking with confirmation number "+bookingConfirmationNumber+" has expired as of "+expirationDate+".Unfortunately, this means you are no longer able to attend batches, complete exams, or receive the certificate due to the expired status.\n" +
                "                </p>\n" +
                "                <p>\n" +
                "                    If you have any questions or wish to discuss your options, please contact our admin team as soon as possible.\n" +
                "                </p>\n" +
                "                <p>Best regards,</p>\n" +
                "                <p class=\"boldText\">SPRK Technologies</p>\n" +
                "                <p class=\"footerlinks\">\n" +
                "                  <a href=\"https://sprktechnologies.in/home\">SPRK Website</a> |\n" +
                "                  <a href=\"http://wa.me/919082572832?text=Hello \">Contact Us</a> |\n" +
                "                  <a href=\"mailto:sprktechnologies.kharghar@gmail.com \">Support</a>\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "          <table\n" +
                "            cellspacing=\"0\"\n" +
                "            cellpadding=\"0\"\n" +
                "            border=\"0\"\n" +
                "            width=\"100%\"\n" +
                "            class=\"letterTable\"\n" +
                "          >\n" +
                "            <tr>\n" +
                "              <td>\n" +
                "                <p class=\"horizontalLine\"></p>\n" +
                "                <p class=\"boldText\">Social Handles</p>\n" +
                "                <p>\n" +
                "                  Follow us on\n" +
                "                  <a href=\"https://www.instagram.com/sprktech/?hl=en\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/skill-icons_instagram_iee7mt.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />@sprktech</a>\n" +
                "                </p>\n" +
                "                <p>\n" +
                "                  Contact us on\n" +
                "                  <a href=\"https://in.linkedin.com/company/sprk-technologies\"\n" +
                "                    ><img\n" +
                "                      src=\"https://res.cloudinary.com/dxlzzgbfw/image/upload/v1701518175/devicon_linkedin_hfekat.png\"\n" +
                "                      class=\"socialIcon\"\n" +
                "                    />SPRK Technologies</a>\n" +
                "                </p>\n" +
                "                <p class=\"footerText\">\n" +
                "                  Address: SPRK Technologies, Sector 3, Kharghar, Navi Mumbai 410210,\n" +
                "                  Maharashtra, India\n" +
                "                </p>\n" +
                "                <p class=\"footerText\">Phone: +91 9082572832</p>\n" +
                "                <p class=\"footerText\">\n" +
                "                  Email:\n" +
                "                  <a href=\"mailto:sprktechnologies.kharghar@gmail.com\"\n" +
                "                    >sprktechnologies.kharghar@gmail.com</a\n" +
                "                  >\n" +
                "                </p>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          </table>\n" +
                "        </td>\n" +
                "      </tr>\n" +
                "    </table>\n" +
                "  </body>\n" +
                "</html>";

        // Create the email template DTO

        return EmailTemplateDTO.builder()
                .recipient(recipientEmail)
                .subject(subject)
                .messageBody(messageBody)
                .isHtml(true)
                .build();
    }






}
