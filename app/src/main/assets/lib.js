/*
 * The MIT License
 *
 * Copyright 2017 Osric Wilkinson (osric@fluffypeople.com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

const BACKEND = 'https://moosemorals.com/link-share/';

function $(selector, base) {
    base = base || document;
    return base.querySelector(selector);
}

function $$(selector, base) {
    base = base || document;
    return base.querySelectorAll(selector);
}

function textNode(text) {
    return document.createTextNode(text);
}

// Copied from https://stackoverflow.com/a/15030117/195833
function flatten(arr) {
    return arr.reduce(function (flat, toFlatten) {
        return flat.concat(Array.isArray(toFlatten) ? flatten(toFlatten) : toFlatten);
    }, []);
}

function closest(node, selector) {
    let target = node;

    while (target !== null && target !== document.documentElement) {
        if (target.matches(selector)) {
            return target;
        }
        target = target.parentNode;
    }
    return undefined;
}

function buildElement(tag, classes, ...args) {
    const el = document.createElement(tag);

    if (classes) {
        classes = classes.split(/\s+/);
        for (let i = 0; i < classes.length; i += 1) {
            if (classes[i]) {
                el.classList.add(classes[i]);
            }
        }
    }

    args = flatten(args);

    for (let index = 0; index < args.length; index += 1) {
        switch (typeof args[index]) {
            case 'undefined':
                // skip it
                break;
            case 'string':
            case 'number':
                el.appendChild(textNode(args[index]));
                break;
            default:
                el.appendChild(args[index]);
                break;
        }
    }
    return el;
}

function getTabInfo() {
    return new Promise(function (resolve, reject) {
        chrome.tabs.query({
            currentWindow: true,
            active: true
        }, tabs => {
            const tab = tabs[0];
            resolve({
                title: tab.title,
                url: tab.url,
                favIconURL: tab.favIconUrl
            });
        })
    });
}

async function sendLink(linkData) {
    const body = new URLSearchParams();
    for (let key in linkData) {
        body.append(key, linkData[key]);
    }

    const creds = await LinkStore.getCredentials();
    return fetch(BACKEND + 'submit?_t=' + creds.token, {
        method: "POST",
        body: body
    });
}

function replaceContent(node, ...content) {
    while (node.lastChild) {
        node.removeChild(node.lastChild);
    }

    if (content === undefined) {
        return;
    }

    for (let i = 0; i < content.length; i += 1) {
        const c = content[i];
        switch (typeof c) {
            case "undefined":
                // skip it
                break;
            case "string":
            case "number":
                node.appendChild(textNode(c));
                break;
            default:
                node.appendChild(c);
                break;
        }
    }
}
