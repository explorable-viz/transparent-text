import { runTests, testURL, waitForFigure } from "./shared/webtest-lib.js";

export const main = async () => {
    await runTests(testURL("figure-spm4b")([ (page) => waitForFigure(page)("fig-leftBarChart") ]))();
    console.log("Success!");
};
