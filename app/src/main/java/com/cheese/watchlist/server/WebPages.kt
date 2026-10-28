package com.cheese.watchlist.server

/**
 * Self-contained HTML/CSS/JS for the PC Manager web pages. Deliberately has
 * zero external dependencies (no CDN scripts/styles) so it keeps working even
 * when the PC has no internet access — only a local WiFi link to the phone.
 */
object WebPages {

    private val sharedStyles = """
        <style>
            :root {
                --bg: #F8F7FC;
                --card: #FFFFFF;
                --accent: #4F6FF5;
                --accent-dark: #3B57D6;
                --border: #E7E5F0;
                --text: #1C1C28;
                --text-secondary: #8B8A9A;
                --star: #FFC94A;
                --danger: #E0616B;
                --success: #3DBE7E;
            }
            * { box-sizing: border-box; }
            body {
                margin: 0;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                background: var(--bg);
                color: var(--text);
            }
            a { color: var(--accent); text-decoration: none; }
            button {
                font-family: inherit;
                cursor: pointer;
                border: none;
                border-radius: 14px;
            }
            .btn-primary {
                background: var(--accent);
                color: #fff;
                padding: 12px 20px;
                font-size: 15px;
                font-weight: 600;
            }
            .btn-primary:hover { background: var(--accent-dark); }
            .btn-secondary {
                background: var(--card);
                color: var(--text);
                border: 1px solid var(--border);
                padding: 12px 20px;
                font-size: 15px;
                font-weight: 600;
            }
            .btn-danger {
                background: var(--danger);
                color: #fff;
                padding: 12px 20px;
                font-size: 15px;
                font-weight: 600;
            }
            .card {
                background: var(--card);
                border: 1px solid var(--border);
                border-radius: 20px;
            }
            input[type=text], input[type=password], input[type=number], select, textarea {
                width: 100%;
                padding: 10px 14px;
                border-radius: 12px;
                border: 1px solid var(--border);
                font-size: 14px;
                font-family: inherit;
                color: var(--text);
                background: #fff;
            }
            input:focus, select:focus, textarea:focus {
                outline: none;
                border-color: var(--accent);
            }
            label {
                font-size: 12px;
                font-weight: 600;
                color: var(--text-secondary);
                text-transform: uppercase;
                letter-spacing: 0.02em;
            }

            /* Modal (used for alerts + delete confirm + add/edit form) */
            .modal-overlay {
                display: none;
                position: fixed;
                inset: 0;
                background: rgba(20, 20, 30, 0.45);
                align-items: center;
                justify-content: center;
                z-index: 1000;
                padding: 20px;
            }
            .modal-overlay.open { display: flex; }
            .modal-box {
                background: #fff;
                border-radius: 22px;
                padding: 28px;
                max-width: 420px;
                width: 100%;
                max-height: 88vh;
                overflow-y: auto;
                box-shadow: 0 20px 60px rgba(20,20,40,0.25);
            }
            .modal-icon {
                width: 56px;
                height: 56px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 26px;
                margin: 0 auto 14px auto;
            }
            .modal-icon.warn { background: #FFF3D6; color: #C9891A; }
            .modal-icon.error { background: #FCE4E6; color: var(--danger); }
            .modal-icon.success { background: #E3F7EC; color: var(--success); }
            .modal-title { font-size: 19px; font-weight: 700; text-align: center; margin: 0 0 6px 0; }
            .modal-message { font-size: 14px; color: var(--text-secondary); text-align: center; margin: 0 0 20px 0; }

            /* Toast */
            .toast {
                position: fixed;
                bottom: 24px;
                right: 24px;
                background: var(--text);
                color: #fff;
                padding: 12px 18px;
                border-radius: 14px;
                font-size: 14px;
                font-weight: 600;
                opacity: 0;
                transform: translateY(10px);
                transition: opacity 0.25s ease, transform 0.25s ease;
                z-index: 2000;
                pointer-events: none;
            }
            .toast.show { opacity: 1; transform: translateY(0); }
        </style>
    """.trimIndent()

    /** Small reusable JS modal/toast system — visually similar to SweetAlert2, zero dependencies. */
    private val sharedScript = """
        <script>
            function showAlert(type, title, message) {
                var overlay = document.getElementById('alertOverlay');
                var icon = document.getElementById('alertIcon');
                var titleEl = document.getElementById('alertTitle');
                var msgEl = document.getElementById('alertMessage');
                icon.className = 'modal-icon ' + type;
                icon.innerHTML = type === 'success' ? '&#10003;' : (type === 'warn' ? '!' : '&#10007;');
                titleEl.textContent = title;
                msgEl.textContent = message;
                overlay.className = 'modal-overlay open';
            }
            function closeAlert() {
                document.getElementById('alertOverlay').className = 'modal-overlay';
            }
            function showToast(message) {
                var toast = document.getElementById('toast');
                toast.textContent = message;
                toast.className = 'toast show';
                setTimeout(function () { toast.className = 'toast'; }, 2200);
            }
        </script>
    """.trimIndent()

    private val alertModalHtml = """
        <div class="modal-overlay" id="alertOverlay">
            <div class="modal-box">
                <div class="modal-icon" id="alertIcon"></div>
                <p class="modal-title" id="alertTitle"></p>
                <p class="modal-message" id="alertMessage"></p>
                <button class="btn-primary" style="width:100%" onclick="closeAlert()">OK</button>
            </div>
        </div>
        <div class="toast" id="toast"></div>
    """.trimIndent()

    fun loginPage(error: String?): String {
        val errorScript = when (error) {
            "empty" -> "showAlert('warn', 'Passcode required', 'Please enter your PC Manager passcode.');"
            "wrong" -> "showAlert('error', 'Invalid passcode', 'Please check the code on your Android device.');"
            else -> ""
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Watchlist - PC Manager</title>
                $sharedStyles
            </head>
            <body>
                <div style="min-height:100vh; display:flex; align-items:center; justify-content:center;">
                    <div class="card" style="max-width:380px; width:100%; padding:36px 32px; text-align:center;">
                        <h1 style="margin:0; font-size:26px;">Watchlist</h1>
                        <p style="color:var(--text-secondary); margin:4px 0 24px 0;">PC Manager</p>
                        <p style="font-size:14px; color:var(--text); margin-bottom:14px;">Enter your passcode</p>
                        <input type="password" id="passcodeInput" maxlength="6" inputmode="numeric"
                            style="text-align:center; font-size:22px; letter-spacing:8px; padding:14px;" />
                        <button class="btn-primary" style="width:100%; margin-top:18px;" onclick="submitPasscode()">Continue</button>
                        <p style="font-size:12px; color:var(--text-secondary); margin-top:20px;">
                            Only use this address on a trusted network.
                        </p>
                    </div>
                </div>
                $alertModalHtml
                $sharedScript
                <script>
                    function submitPasscode() {
                        var value = document.getElementById('passcodeInput').value.trim();
                        if (!value) {
                            showAlert('warn', 'Passcode required', 'Please enter your PC Manager passcode.');
                            return;
                        }
                        fetch('/login', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ passcode: value })
                        }).then(function (res) {
                            if (res.ok) {
                                window.location.href = '/';
                            } else {
                                showAlert('error', 'Invalid passcode', 'Please check the code on your Android device.');
                            }
                        }).catch(function () {
                            showAlert('error', 'Connection error', 'Could not reach the phone. Try again.');
                        });
                    }
                    document.getElementById('passcodeInput').addEventListener('keydown', function (e) {
                        if (e.key === 'Enter') submitPasscode();
                    });
                    $errorScript
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun dashboardPage(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Watchlist - PC Manager</title>
                $sharedStyles
                <style>
                    .topbar {
                        background: var(--card);
                        border-bottom: 1px solid var(--border);
                        padding: 16px 28px;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                    }
                    .container { max-width: 1100px; margin: 0 auto; padding: 24px 20px 100px 20px; }
                    .filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 18px 0; }
                    .chip {
                        border: 1px solid var(--border);
                        background: #fff;
                        border-radius: 999px;
                        padding: 8px 16px;
                        font-size: 13px;
                        font-weight: 600;
                        color: var(--text);
                        cursor: pointer;
                    }
                    .chip.active { background: var(--accent); color: #fff; border-color: var(--accent); }
                    .grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                        gap: 16px;
                    }
                    .item-card { padding: 18px; }
                    .item-title { font-size: 16px; font-weight: 700; margin: 0 0 2px 0; }
                    .item-meta { font-size: 13px; color: var(--text-secondary); margin: 2px 0; }
                    .stars { color: var(--star); font-size: 15px; letter-spacing: 1px; }
                    .fab {
                        position: fixed;
                        bottom: 28px;
                        right: 28px;
                        width: 56px;
                        height: 56px;
                        border-radius: 50%;
                        background: var(--accent);
                        color: #fff;
                        font-size: 28px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 10px 24px rgba(79,111,245,0.4);
                    }
                    .row-between { display: flex; align-items: center; justify-content: space-between; }
                    .icon-btn {
                        background: transparent;
                        border: none;
                        font-size: 16px;
                        color: var(--text-secondary);
                        padding: 4px 8px;
                    }
                    .progress-track { background: var(--border); border-radius: 8px; height: 8px; overflow: hidden; margin: 6px 0; }
                    .progress-fill { background: var(--accent); height: 100%; }
                    .field-group { margin-bottom: 14px; }
                    .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
                </style>
            </head>
            <body>
                <div class="topbar">
                    <div>
                        <strong style="font-size:18px;">Watchlist</strong>
                        <span style="color:var(--text-secondary); font-size:13px; margin-left:6px;">PC Manager</span>
                    </div>
                    <a href="/logout" style="font-size:13px; font-weight:600;">Log out</a>
                </div>

                <div class="container">
                    <input type="text" id="searchInput" placeholder="Search your watchlist..." oninput="applyFilters()" />
                    <div class="filters" id="statusFilters"></div>
                    <div class="filters" id="genreFilters"></div>
                    <div class="grid" id="itemsGrid"></div>
                </div>

                <button class="fab" onclick="openForm(null)">+</button>

                <div class="modal-overlay" id="deleteOverlay">
                    <div class="modal-box">
                        <div class="modal-icon error">!</div>
                        <p class="modal-title">Delete this title?</p>
                        <p class="modal-message">This can't be undone.</p>
                        <div style="display:flex; gap:10px;">
                            <button class="btn-secondary" style="flex:1" onclick="closeDeleteConfirm()">Cancel</button>
                            <button class="btn-danger" style="flex:1" onclick="confirmDelete()">Delete</button>
                        </div>
                    </div>
                </div>

                <div class="modal-overlay" id="formOverlay">
                    <div class="modal-box" style="max-width:480px;">
                        <p class="modal-title" id="formTitle" style="text-align:left;">Add Title</p>
                        <div class="field-group">
                            <label>Title</label>
                            <input type="text" id="f_title" />
                        </div>
                        <div class="two-col field-group">
                            <div>
                                <label>Type</label>
                                <select id="f_type" onchange="onTypeChange()">
                                    <option value="MOVIE">Movie</option>
                                    <option value="SERIES">Series</option>
                                </select>
                            </div>
                            <div>
                                <label>Status</label>
                                <select id="f_status">
                                    <option value="TO_WATCH">To Watch</option>
                                    <option value="WATCHING">Watching</option>
                                    <option value="WATCHED">Watched</option>
                                    <option value="DROPPED">Dropped</option>
                                </select>
                            </div>
                        </div>
                        <div class="two-col field-group">
                            <div>
                                <label>Genre</label>
                                <select id="f_genre"></select>
                            </div>
                            <div>
                                <label>Where to Watch</label>
                                <select id="f_source"></select>
                            </div>
                        </div>
                        <div id="seriesFields">
                            <div class="two-col field-group">
                                <div>
                                    <label>Season</label>
                                    <input type="number" id="f_season" min="1" value="1" />
                                </div>
                                <div>
                                    <label>Total Episodes</label>
                                    <input type="number" id="f_total" min="0" value="0" />
                                </div>
                            </div>
                            <div class="field-group">
                                <label>Current Episode</label>
                                <input type="number" id="f_current" min="0" value="0" />
                            </div>
                        </div>
                        <div class="field-group">
                            <label>URL</label>
                            <input type="text" id="f_url" placeholder="https://example.com/watch" />
                        </div>
                        <div class="field-group">
                            <label>Year / Date</label>
                            <input type="text" id="f_year" />
                        </div>
                        <div class="field-group">
                            <label>Rating</label>
                            <div id="f_ratingStars" class="stars" style="cursor:pointer; font-size:22px;"></div>
                        </div>
                        <div class="field-group">
                            <label><input type="checkbox" id="f_favorite" style="width:auto;" /> Favorite</label>
                        </div>
                        <div class="field-group">
                            <label>Comments</label>
                            <textarea id="f_comments" rows="3"></textarea>
                        </div>
                        <div style="display:flex; gap:10px; margin-top:6px;">
                            <button class="btn-secondary" style="flex:1" onclick="closeForm()">Cancel</button>
                            <button class="btn-primary" style="flex:1" onclick="submitForm()">Save</button>
                        </div>
                    </div>
                </div>

                $alertModalHtml
                $sharedScript
                <script>
                    var allItems = [];
                    var allGenres = [];
                    var allSources = [];
                    var statusFilter = 'ALL';
                    var typeFilter = 'ALL';
                    var favoritesOnly = false;
                    var genreFilter = null;
                    var editingId = null;
                    var deleteTargetId = null;
                    var currentRating = 0;

                    var statusOptions = [
                        { key: 'ALL', label: 'All' },
                        { key: 'FAVORITES', label: 'Favorites' },
                        { key: 'MOVIE', label: 'Movies' },
                        { key: 'SERIES', label: 'Series' },
                        { key: 'TO_WATCH', label: 'To Watch' },
                        { key: 'WATCHING', label: 'Watching' },
                        { key: 'WATCHED', label: 'Watched' },
                        { key: 'DROPPED', label: 'Dropped' }
                    ];

                    function loadAll() {
                        fetch('/api/watchlist').then(function (r) { return r.json(); }).then(function (data) {
                            allItems = data;
                            render();
                        });
                        fetch('/api/genres').then(function (r) { return r.json(); }).then(function (data) {
                            allGenres = data;
                            renderGenreFilters();
                            renderGenreOptions();
                        });
                        fetch('/api/sources').then(function (r) { return r.json(); }).then(function (data) {
                            allSources = data;
                            renderSourceOptions();
                        });
                    }

                    function renderStatusFilters() {
                        var el = document.getElementById('statusFilters');
                        el.innerHTML = '';
                        statusOptions.forEach(function (opt) {
                            var chip = document.createElement('div');
                            var active = (opt.key === 'ALL' && statusFilter === 'ALL' && typeFilter === 'ALL' && !favoritesOnly) ||
                                (opt.key === 'FAVORITES' && favoritesOnly) ||
                                (opt.key === typeFilter) ||
                                (opt.key === statusFilter);
                            chip.className = 'chip' + (active ? ' active' : '');
                            chip.textContent = opt.label;
                            chip.onclick = function () {
                                favoritesOnly = false; typeFilter = 'ALL'; statusFilter = 'ALL';
                                if (opt.key === 'FAVORITES') favoritesOnly = true;
                                else if (opt.key === 'MOVIE' || opt.key === 'SERIES') typeFilter = opt.key;
                                else if (opt.key !== 'ALL') statusFilter = opt.key;
                                renderStatusFilters();
                                applyFilters();
                            };
                            el.appendChild(chip);
                        });
                    }

                    function renderGenreFilters() {
                        var el = document.getElementById('genreFilters');
                        el.innerHTML = '';
                        var all = document.createElement('div');
                        all.className = 'chip' + (genreFilter === null ? ' active' : '');
                        all.textContent = 'All Genres';
                        all.onclick = function () { genreFilter = null; renderGenreFilters(); applyFilters(); };
                        el.appendChild(all);
                        allGenres.forEach(function (g) {
                            var chip = document.createElement('div');
                            chip.className = 'chip' + (genreFilter === g ? ' active' : '');
                            chip.textContent = g;
                            chip.onclick = function () { genreFilter = g; renderGenreFilters(); applyFilters(); };
                            el.appendChild(chip);
                        });
                    }

                    function renderGenreOptions() {
                        var sel = document.getElementById('f_genre');
                        sel.innerHTML = '';
                        allGenres.forEach(function (g) {
                            var o = document.createElement('option');
                            o.value = g; o.textContent = g;
                            sel.appendChild(o);
                        });
                    }

                    function renderSourceOptions() {
                        var sel = document.getElementById('f_source');
                        sel.innerHTML = '';
                        allSources.forEach(function (s) {
                            var o = document.createElement('option');
                            o.value = s; o.textContent = s;
                            sel.appendChild(o);
                        });
                    }

                    function applyFilters() {
                        var q = document.getElementById('searchInput').value.toLowerCase();
                        var filtered = allItems.filter(function (item) {
                            var matchesSearch = !q ||
                                item.title.toLowerCase().indexOf(q) !== -1 ||
                                item.genre.toLowerCase().indexOf(q) !== -1 ||
                                item.type.toLowerCase().indexOf(q) !== -1 ||
                                item.whereToWatch.toLowerCase().indexOf(q) !== -1;
                            var matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
                            var matchesType = typeFilter === 'ALL' || item.type === typeFilter;
                            var matchesFav = !favoritesOnly || item.favorite;
                            var matchesGenre = !genreFilter || item.genre === genreFilter;
                            return matchesSearch && matchesStatus && matchesType && matchesFav && matchesGenre;
                        });
                        renderGrid(filtered);
                    }

                    function render() {
                        renderStatusFilters();
                        applyFilters();
                    }

                    function starString(rating) {
                        var s = '';
                        for (var i = 1; i <= 5; i++) s += (i <= rating ? String.fromCharCode(9733) : String.fromCharCode(9734));
                        return s;
                    }

                    function renderGrid(items) {
                        var grid = document.getElementById('itemsGrid');
                        grid.innerHTML = '';
                        if (items.length === 0) {
                            grid.innerHTML = '<p style="color:var(--text-secondary);">No titles match your filters.</p>';
                            return;
                        }
                        items.forEach(function (item) {
                            var card = document.createElement('div');
                            card.className = 'card item-card';

                            var html = '<div class="row-between">';
                            html += '<p class="item-title">' + escapeHtml(item.title) + '</p>';
                            html += '<button class="icon-btn" onclick="toggleFav(' + item.id + ')">' + (item.favorite ? String.fromCharCode(9733) : String.fromCharCode(9734)) + '</button>';
                            html += '</div>';
                            html += '<p class="item-meta">' + (item.type === 'MOVIE' ? 'Movie' : 'Series') + ' &middot; ' + item.statusLabel + '</p>';
                            if (item.genre) html += '<p class="item-meta">' + escapeHtml(item.genre) + '</p>';
                            if (item.rating > 0) html += '<p class="stars">' + starString(item.rating) + '</p>';

                            if (item.type === 'SERIES' && item.totalEpisodes > 0) {
                                var pct = Math.round((item.currentEpisode / item.totalEpisodes) * 100);
                                html += '<p class="item-meta">Season ' + item.season + '</p>';
                                html += '<div class="row-between"><span class="item-meta">Episode ' + item.currentEpisode + '/' + item.totalEpisodes + '</span><span class="item-meta">' + pct + '%</span></div>';
                                html += '<div class="progress-track"><div class="progress-fill" style="width:' + pct + '%;"></div></div>';
                                html += '<div style="display:flex; gap:8px; margin-top:6px;">';
                                html += '<button class="btn-secondary" onclick="adjustEpisode(' + item.id + ', -1)" style="flex:1; padding:6px;">-</button>';
                                html += '<button class="btn-secondary" onclick="adjustEpisode(' + item.id + ', 1)" style="flex:1; padding:6px;">+</button>';
                                html += '</div>';
                            }

                            if (item.url) {
                                html += '<p style="margin-top:8px;"><a href="' + escapeAttr(item.url) + '" target="_blank" rel="noopener noreferrer">' + escapeHtml(item.url) + '</a></p>';
                            }

                            html += '<div style="display:flex; gap:8px; margin-top:12px;">';
                            html += '<button class="btn-secondary" style="flex:1;" onclick="openForm(' + item.id + ')">Edit</button>';
                            html += '<button class="btn-secondary" style="flex:1; color:var(--danger);" onclick="openDeleteConfirm(' + item.id + ')">Delete</button>';
                            html += '</div>';

                            card.innerHTML = html;
                            grid.appendChild(card);
                        });
                    }

                    function escapeHtml(str) {
                        var div = document.createElement('div');
                        div.textContent = str;
                        return div.innerHTML;
                    }
                    function escapeAttr(str) {
                        return String(str).replace(/"/g, '&quot;');
                    }

                    function toggleFav(id) {
                        fetch('/api/watchlist/' + id + '/favorite', { method: 'PUT' })
                            .then(function () { loadAll(); });
                    }

                    function adjustEpisode(id, delta) {
                        fetch('/api/watchlist/' + id + '/episode', {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ delta: delta })
                        }).then(function () { loadAll(); });
                    }

                    function openDeleteConfirm(id) {
                        deleteTargetId = id;
                        document.getElementById('deleteOverlay').className = 'modal-overlay open';
                    }
                    function closeDeleteConfirm() {
                        document.getElementById('deleteOverlay').className = 'modal-overlay';
                    }
                    function confirmDelete() {
                        fetch('/api/watchlist/' + deleteTargetId, { method: 'DELETE' }).then(function () {
                            closeDeleteConfirm();
                            showToast('Deleted');
                            loadAll();
                        });
                    }

                    function onTypeChange() {
                        var isSeries = document.getElementById('f_type').value === 'SERIES';
                        document.getElementById('seriesFields').style.display = isSeries ? 'block' : 'none';
                    }

                    function renderRatingPicker() {
                        var el = document.getElementById('f_ratingStars');
                        el.innerHTML = '';
                        for (var i = 1; i <= 5; i++) {
                            var span = document.createElement('span');
                            span.textContent = (i <= currentRating) ? String.fromCharCode(9733) : String.fromCharCode(9734);
                            span.style.marginRight = '4px';
                            span.onclick = (function (val) { return function () { currentRating = val; renderRatingPicker(); }; })(i);
                            el.appendChild(span);
                        }
                    }

                    function openForm(id) {
                        editingId = id;
                        document.getElementById('formTitle').textContent = id ? 'Edit Title' : 'Add Title';
                        if (id) {
                            var item = allItems.find(function (it) { return it.id === id; });
                            document.getElementById('f_title').value = item.title;
                            document.getElementById('f_type').value = item.type;
                            document.getElementById('f_status').value = item.status;
                            document.getElementById('f_genre').value = item.genre;
                            document.getElementById('f_source').value = item.whereToWatch;
                            document.getElementById('f_season').value = item.season;
                            document.getElementById('f_total').value = item.totalEpisodes;
                            document.getElementById('f_current').value = item.currentEpisode;
                            document.getElementById('f_url').value = item.url;
                            document.getElementById('f_year').value = item.yearDate;
                            document.getElementById('f_favorite').checked = item.favorite;
                            document.getElementById('f_comments').value = item.comments;
                            currentRating = item.rating;
                        } else {
                            document.getElementById('f_title').value = '';
                            document.getElementById('f_type').value = 'MOVIE';
                            document.getElementById('f_status').value = 'TO_WATCH';
                            document.getElementById('f_season').value = 1;
                            document.getElementById('f_total').value = 0;
                            document.getElementById('f_current').value = 0;
                            document.getElementById('f_url').value = '';
                            document.getElementById('f_year').value = '';
                            document.getElementById('f_favorite').checked = false;
                            document.getElementById('f_comments').value = '';
                            currentRating = 0;
                        }
                        onTypeChange();
                        renderRatingPicker();
                        document.getElementById('formOverlay').className = 'modal-overlay open';
                    }
                    function closeForm() {
                        document.getElementById('formOverlay').className = 'modal-overlay';
                    }

                    function submitForm() {
                        var title = document.getElementById('f_title').value.trim();
                        if (!title) {
                            showAlert('warn', 'Title required', 'Please enter a title before saving.');
                            return;
                        }
                        var payload = {
                            title: title,
                            type: document.getElementById('f_type').value,
                            status: document.getElementById('f_status').value,
                            genre: document.getElementById('f_genre').value || '',
                            whereToWatch: document.getElementById('f_source').value || '',
                            season: parseInt(document.getElementById('f_season').value || '1', 10),
                            totalEpisodes: parseInt(document.getElementById('f_total').value || '0', 10),
                            currentEpisode: parseInt(document.getElementById('f_current').value || '0', 10),
                            url: document.getElementById('f_url').value.trim(),
                            yearDate: document.getElementById('f_year').value.trim(),
                            favorite: document.getElementById('f_favorite').checked,
                            comments: document.getElementById('f_comments').value,
                            rating: currentRating
                        };
                        var url = editingId ? ('/api/watchlist/' + editingId) : '/api/watchlist';
                        var method = editingId ? 'PUT' : 'POST';
                        fetch(url, {
                            method: method,
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload)
                        }).then(function (res) {
                            if (!res.ok) throw new Error('save failed');
                            return res.json();
                        }).then(function () {
                            closeForm();
                            showToast(editingId ? 'Updated' : 'Saved');
                            loadAll();
                        }).catch(function () {
                            showAlert('error', 'Could not save', 'Please check your entries and try again.');
                        });
                    }

                    loadAll();
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
