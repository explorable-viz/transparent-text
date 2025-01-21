## Creating Websites
1. Create a folder in `promptExecutor/website/`: `user:/path/to/repo/transparent-text/promptExecutor> mkdir website/$WEBSITE_NAME`
2. For each subpage, create an html file: `user:/path/to/repo/transparent-text/promptExecutor> touch website/$WEBSITE_NAME/$PAGE_NAME.html$`
3. Create the spec for your fluid program: `user:/path/to/repo/transparent-text/promptExecutor> touch website/$WEBSITE_NAME/$PAGE_NAME.json`
4. Configure the json file appropriately, for examples look in `promptExecutor/website/Ar6Wg1SPM` 
5. At the bottom of your html file, include a script to load your fluid program as a figure:
```
</body>
...
<script type="module">
   import { loadFigure } from "../shared/load-figure.js";
   loadFigure("spec.json")();
</script>
</body>
</html>
```
6. Create symlinks to the following files/folders in `$WEBSITE_NAME`: `website/css`,  `website/font`, `website/image`, `website/shared`, `website/favicon.ico`
7. (Optional): include a `test.mjs` file in the root of your website's directory, for an example of what this looks like, see `promptExecutor/website/AR6Wg1SPM/test.mjs` 
## Bundling/Testing Websites
To bundle a website, run the command `yarn fluid publish -w $WEBSITE_NAME -L` from `promptExecutor/`.
This will create a folder in `promptExecutor/dist` with a lisp-cased version of `$WEBSITE_NAME`.

To test the website in the browser: 
1. run `npx http-serve dist/$WEBSITE_NAME_LISP_CASE -a 127.0.0.1 -c-1`
2. Open your browser, and navigate to the available address shown in your terminal, e.g: `https://127.0.0.1:8080/$PAGE_NAME_LISP_CASE`

To run `test.mjs`:
1. Run the command `user:/path/to/repo/transparent-text/promptExecutor> yarn website-test $WEBSITE_NAME_LISP_CASE`
