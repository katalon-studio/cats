package com.endava.cats.model;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a summary of a CATS test case.
 * This summary includes information such as test case status, path, and execution time.
 */
@EqualsAndHashCode
@Getter
public class CatsTestCaseSummary implements Comparable<CatsTestCaseSummary> {
    private String scenario;
    private String result;
    private String resultReason;
    private String id;
    private String fuzzer;
    private String path;
    private String resultDetails;
    private String expectedResult;
    private String curl;
    private double timeToExecuteInSec;
    private String httpMethod;
    private boolean switchedResult;

    /**
     * Creates a CatsTestCaseSummary object from a CatsTestCase.
     *
     * @param testCase The CatsTestCase to generate the summary from.
     * @return A CatsTestCaseSummary representing the summary of the provided CatsTestCase.
     */
    public static CatsTestCaseSummary fromCatsTestCase(CatsTestCase testCase) {
        CatsTestCaseSummary summary = new CatsTestCaseSummary();
        summary.id = testCase.getTestId();
        summary.scenario = testCase.getScenario();
        summary.result = testCase.getResult();
        summary.fuzzer = testCase.getFuzzer();
        summary.path = testCase.getContractPath();
        summary.resultReason = testCase.getResultReason();
        summary.resultDetails = testCase.getResultDetails();
        summary.expectedResult = testCase.getExpectedResult();
        summary.curl = sanitizeCurl(testCase.getCurl());

        CatsResponse response = testCase.getResponse() == null ? CatsResponse.empty() : testCase.getResponse();
        CatsRequest request = testCase.getRequest() == null ? CatsRequest.empty() : testCase.getRequest();

        summary.timeToExecuteInSec = response.getResponseTimeInMs() / 1000d;
        String method = request.getHttpMethod();
        summary.httpMethod = method == null ? "####" : method.toLowerCase(Locale.ROOT);
        summary.switchedResult = testCase.getResultIgnoreDetails() != null;

        return summary;
    }

    /**
     * Removes the User-Agent header and minifies any JSON payload within the curl command.
     *
     * @param curl the original curl command
     * @return sanitized curl command
     */
    private static String sanitizeCurl(String curl) {
        if (curl == null) {
            return null;
        }

        String withoutUserAgent = curl.replaceAll("\\s-?H \"User-Agent:[^\"]*\"", "");
        return minifyCurlPayload(withoutUserAgent);
    }

    /**
     * Detects a -d 'payload' section and minifies the JSON content if possible.
     * If the payload is not valid JSON, whitespace is compacted to a single space.
     */
    private static String minifyCurlPayload(String curl) {
        Pattern payloadPattern = Pattern.compile("(-d ')\\s*(.*?)\\s*(')", Pattern.DOTALL);
        Matcher matcher = payloadPattern.matcher(curl);

        if (matcher.find()) {
            String body = matcher.group(2);
            String minifiedBody = body;

            try {
                minifiedBody = new Gson().toJson(JsonParser.parseString(body));
            } catch (JsonSyntaxException ex) {
                minifiedBody = body.replaceAll("\\s+", " ");
            }

            return matcher.replaceFirst("$1" + Matcher.quoteReplacement(minifiedBody) + "$3");
        }

        return curl;
    }

    @Override
    public int compareTo(CatsTestCaseSummary o) {
        String o1StringPart = this.id.replaceAll("\\d", "");
        String o2StringPart = o.id.replaceAll("\\d", "");

        if (o1StringPart.equalsIgnoreCase(o2StringPart)) {
            return extractInt(this.id) - extractInt(o.id);
        }
        return this.id.compareTo(o.id);
    }

    private int extractInt(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    /**
     * Gets a key derived from the test ID with spaces removed.
     *
     * @return The key for the test.
     */
    public String getKey() {
        return id.replace(" ", "");
    }

    /**
     * Checks if the test result is an error.
     *
     * @return True if the result is an error, false otherwise.
     */
    public boolean getError() {
        return this.result.equalsIgnoreCase("error");
    }

    /**
     * Checks if the test result is a warning.
     *
     * @return True if the result is a warning, false otherwise.
     */
    public boolean getWarning() {
        return this.result.equalsIgnoreCase("warning");
    }
}
