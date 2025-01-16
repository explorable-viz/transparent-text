import puppeteer from "puppeteer";

export const main = async () => {
    const browser = await puppeteer.launch();
    await browser.close();
    console.log("Success!");
};
