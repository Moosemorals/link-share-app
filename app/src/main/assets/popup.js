/*
 * ISC License
 *
 * Copyright (c) 2017 Osric Wilkinson (osric@fluffypeople.com)
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

const BLANK_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg'/%3E";

import * as LinkStore from './LinkStore.js';
import {
    BACKEND,
    buildElement,
    $,
    $$,
    replaceContent,
    sendLink,
    getTabInfo,
    closest
} from './lib.js';

var users = [],
    creds = {},
    links = [];

function isForUser(from, to, link) {
    const linkFrom = link.from.toLowerCase();
    const linkTo = link.to.toLowerCase();

    // If the link is  (from me and to you) or (from you and to me)
    // Then we want to include it on this tab
    return (from === linkFrom && to === linkTo) || (from === linkTo && to === linkFrom);
}

function updateListBoxes(list) {

    function _cleanRow(row) {
        row.classList.remove("top");
        row.classList.remove("middle");
        row.classList.remove("bottom");
    }

    function _isMine(row) {
        return row.classList.contains("mine");
    }

    if (!list.firstChild) {
        return;
    }

    const listRows = []
    $$(".row", list).forEach(row => listRows.push(row.parentNode.removeChild(row)));
    replaceContent(list);

    const rowGroups = [];
    let rows = [];
    rows.push(listRows[0]);


    for (let i = 1; i < listRows.length; i += 1) {
        const row = listRows[i];
        _cleanRow(row);

        if (_isMine(row) !== _isMine(rows[rows.length - 1])) {
            rowGroups.push(rows);
            rows = [row];
        } else {
            rows.push(row);
        }
    }

    rowGroups.push(rows);

    rowGroups.forEach(group => {
        const div = buildElement("div", "group");
        if (_isMine(group[0])) {
            div.classList.add("mine");
        }
        group.forEach((row, index) => {
            if (index === 0) {
                row.classList.add("top");
            }
            if (index === group.length - 1) {
                row.classList.add("bottom");
            }

            div.appendChild(row)

        });
        list.appendChild(div);
    })
}

function buildLink(target) {
    return BACKEND + target + "?_t=" + creds.token;
}

function buildRow(link) {

    const linkItself = buildElement("span", "link", "title" in link ? link.title : link.url);

    let favIcon = buildElement("img", "favIcon");
    favIcon.width = 16;
    favIcon.height = 16;
    favIcon.src = link.favIconURL || BLANK_IMAGE;

    const deleteLink = buildElement("a", "delete", "\u2A2F");
    deleteLink.title = "Click to delete this link";
    deleteLink.dataset.id = link.id;


    const parts = [
        favIcon,
        linkItself,
        buildElement("span", "padding"),
        deleteLink
    ];

    const mine = link.from === creds.user;

    if (mine) {
        parts.reverse();
    }

    const classes = [
            "row",
            mine ? "mine" : undefined,
            link.visisted ? "visited" : undefined
        ]
        .filter(x => x !== undefined)
        .join(" ");

    const row = buildElement("div", classes, ...parts);

    row.title = link.url;
    row.dataset.id = link.id;
    return row;
}

function buildTab(index, name, ...content) {
    const input = buildElement("input", "tab-toggle");
    input.type = "radio";
    input.id = "tab-" + index;
    input.name = "tabGroup";

    const label = buildElement("label", "tab-label", name);
    label.setAttribute("for", input.id);

    return buildElement("div", "tab user",
        input,
        label,
        buildElement("div", "content", content)
    );
}

function buildDeviceList(user) {
    const select = buildElement("select", "devices",
        buildElement("option", undefined, "All devices")
    );

    for (let i = 0; i < user.devices.length; i += 1) {
        select.appendChild(
            buildElement("option", undefined, user.devices[i])
        );
    }

    return select;
}

function buildControls(user) {
    const favIcon = buildElement("img", "favIcon");
    favIcon.src = BLANK_IMAGE;
    favIcon.size

    const title = buildElement("input", "title");
    title.type = "text";

    const send = buildElement("button", "send", "Share");
    send.type = "button";

    return buildElement("div", "controls",
        favIcon,
        title,
        buildDeviceList(user),
        send
    );
}

function buildLinks(user) {

    const ids = Object.keys(links)
        .filter(id => isForUser(creds.user.toLowerCase(), user.name.toLowerCase(), links[id]))
        .sort((a, b) => {
            return links[b].created - links[a].created;
        })

    const list = buildElement("div", "list");
    for (let i = 0; i < ids.length; i += 1) {
        const link = links[ids[i]];
        list.appendChild(buildRow(link));
    }
    updateListBoxes(list);
    return list;
}

function buildSetingsTab() {

    var thisUser;
    for (let i = 0; i < users.length; i += 1) {
        if (users[i].name === creds.user) {
            thisUser = users[i];
            break;
        }
    }

    if (thisUser === undefined) {
        return buildElement("div", "warning", "You don't seem to be known to the system");
    }

    const div = buildElement("div", "settings");

    thisUser.devices.forEach(device => {
        const row = buildElement("div");
    })

    const btn = buildElement("button", undefined, "Logout");
    btn.id = "logout";

    div.appendChild(btn);

    return div;
}

function buildTabs(initalTab) {
    const tabs = buildElement("div", "tabs");
    for (let i = 0; i < users.length; i += 1) {
        const user = users[i];

        const tab = buildTab(i, user.name,
            buildControls(user),
            buildLinks(user)
        )

        if (user.name.toLowerCase() === creds.user.toLowerCase()) {
            // The tab for the signed in user always goes first
            tabs.insertBefore(tab, tabs.firstChild);
        } else {
            tabs.appendChild(tab);
        }
    }
    
    tabs.appendChild(buildTab(users.length, "Search", "This will be search"));
    tabs.appendChild(buildTab(users.length + 1, "Settings", buildSetingsTab()));

    if (initalTab !== undefined) {
        $(initalTab + " .tab-toggle").checked = true;
    } else if (tabs.firstChild) {
        $(".tab-toggle", tabs.firstChild).checked = true;
    }

    return tabs;
}

function drawDisplay(initialTab) {
    const tabs = buildTabs(initialTab);

    replaceContent($("article"), tabs);
    getTabInfo().then(tabDetails => {
        $$(".controls .title").forEach(el => el.value = tabDetails.title || tabDetails.url || "");
        $$(".controls .favIcon").forEach(el => el.src = tabDetails.favIconURL || BLANK_IMAGE);
    })
}

function doDelete(target) {
    const id = target.dataset.id;
    const deleteLink = buildLink("links/" + id);
    fetch(deleteLink, {
        method: "DELETE"
    }).then(response => {
        if (response.ok) {
            console.log("Link deleted");
        }
    })
}

function doNavigate(target) {
    const id = target.dataset.id;
    const link = links[id];

    link.visisted = true;

    LinkStore.addLink(link).then(() => {

        $$("[data-id='" + link.id + "']").forEach(el => el.classList.add("visited"));

        chrome.tabs.create({
            url: link.url
        });

    })
}

function doLogout() {
    fetch(buildLink("logout"), {
        method: 'POST'
    }).then(result => {
        if (result.ok) {
            LinkStore.deleteCredentials()
                .then(() => {
                    chrome.storage.local.clear();
                    window.close()
                })
        }
    })
}

function doSend(e) {
    const btn = e.target;
    btn.removeEventListener("click", doSend);
    btn.disabled = true;
    replaceContent(btn, "Sending...");

    const tab = closest(btn, ".tab");
    const controls = $(".controls", tab);

    getTabInfo().then(tabInfo => {
        const linkData = {
            url: tabInfo.url,
            title: $(".title", controls).value,
            to: $("label", tab).firstChild.nodeValue,
            device: $("select", controls).value
        }

        if (tab.favIconURL) {
            linkData.favIconURL = tabInfo.favIconURL;
        }
        
        sendLink(linkData);
    })
}

function enableClick() {
    $$(".send").forEach(btn => {
        btn.addEventListener("click", doSend);
        btn.disabled = false;
        replaceContent(btn, "Share");
    })
}

function getUsers() {
    return fetch(buildLink("users"))
        .then(response => {
            if (response.ok) {
                return response.json().then(json => {
                    if ("success" in json) {
                        return json.success;
                    } else {
                        throw new Error("Problem fetching users");
                    }
                });
            } else {
                throw new Error("Problem fetching users");
            }
        })
}

function onChangeTab() {
    LinkStore.setCurrentTab(this.getAttribute("for"));
}

function setup() {
    LinkStore.watchLinks((newLinks, oldLinks) => {
        newLinks = newLinks || {};
        oldLinks = oldLinks || {};

        const newIds = new Set(Object.keys(newLinks));
        const oldIds = new Set(Object.keys(oldLinks));

        if (newIds.size > oldIds.size) {
            // New link
            const delta = newIds.difference(oldIds);

            delta.forEach(id => {
                const link = newLinks[id];
                links[id] = link;

                $$(".tab.user").forEach(tab => {
                    const name = $("label", tab).firstChild.nodeValue.toLowerCase();

                    if (isForUser(creds.user.toLowerCase(), name, link)) {
                        const row = buildRow(link);
                        const list = $(".list", tab);
                        list.insertBefore(row, list.firstChild);
                    }
                })
            })

            $$(".list").forEach(list => updateListBoxes(list));

            $$(".send").forEach(send => {
                if (send.disabled) {
                    enableClick(send);
                }
            })
        } else if (newIds.size < oldIds.size) {
            // Link removed
            const delta = oldIds.difference(newIds);
            delta.forEach(id => {
                $$("[data-id='" + id + "']").forEach(row => row.parentNode.removeChild(row))
                delete links[id];
            });
        } else {
            console.log("Something changed");
        }
    })

    $("article").addEventListener("click", e => {
        const actions = {
            ".delete": doDelete,
            ".row": doNavigate,
            ".title": e => e.select(),
            "#logout": doLogout,
            ".tab-label": onChangeTab
        };

        let target = e.target;
        while (target !== null && target !== document.documentElement) {
            for (let key in actions) {
                if (target.matches(key)) {
                    e.preventDefault();
                    actions[key](target);
                    return;
                }
            }
            target = target.parentNode;
        }
    }, true);

    Promise.all([
        LinkStore.getLinks(),
        LinkStore.getCredentials(),
        LinkStore.getCurrentTab()
    ]).then(values => {
        let currentTab;
        [links, creds, currentTab] = values;
        getUsers().then(u => {
            users = u;
            LinkStore.resetLinkCount();
            drawDisplay(currentTab);
            enableClick();
        })
    })
}

document.addEventListener("DOMContentLoaded", setup);