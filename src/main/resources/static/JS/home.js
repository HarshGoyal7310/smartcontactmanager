console.log("home.js loaded successfully");

// 1. Responsive Sidebar Toggle
function toggleSidebar() {
    const sidebar = document.querySelector(".sidebar");
    const content = document.querySelector(".content");

    if (sidebar && content) {
        if (window.innerWidth <= 768) {
            sidebar.classList.toggle("sidebar-open");
        } else {
            if (sidebar.style.display === "none") {
                sidebar.style.display = "block";
                content.style.marginLeft = "250px";
            } else {
                sidebar.style.display = "none";
                content.style.marginLeft = "0px";
            }
        }
    }
}

// 2. Mobile par bahar click karne se sidebar close ho jaye
document.addEventListener("click", function (e) {
    if (window.innerWidth <= 768) {
        const sidebar = document.querySelector(".sidebar");
        const toggleBtn = document.querySelector(".sidebar-toggle-btn");
        if (sidebar && sidebar.classList.contains("sidebar-open")) {
            if (!sidebar.contains(e.target) && toggleBtn && !toggleBtn.contains(e.target)) {
                sidebar.classList.remove("sidebar-open");
            }
        }
    }

    // 3-Dots dropdown ke bahar click karne par dropdown close ho jaye
    if (!e.target.closest(".dropdown")) {
        document.querySelectorAll(".dropdown-menu").forEach(function (menu) {
            menu.classList.remove("show");
        });
    }
});

// 3. Live Instant Search Function
function search() {
    let searchInput = document.getElementById("search-input");
    if (!searchInput) return;

    let query = searchInput.value.trim();

    if (query === "") {
        $(".search-result").hide();
    } else {
        let url = `/search/${query}`;

        fetch(url)
            .then((response) => response.json())
            .then((data) => {
                let htmlContent = "<div class='list-group'>";

                if (data.length > 0) {
                    data.forEach((contact) => {
                        htmlContent += `
                            <a href='/user/contact/${contact.cId}' class='list-group-item list-group-item-action text-left'>
                                <i class='fa-solid fa-user mr-2 text-primary'></i>
                                <strong>${contact.name}</strong>
                                <span class='text-muted ml-2'>(${contact.phone})</span>
                            </a>
                        `;
                    });
                } else {
                    htmlContent += `
                        <div class='list-group-item text-muted text-center'>
                            No matching contacts found!
                        </div>
                    `;
                }

                htmlContent += "</div>";

                $(".search-result").html(htmlContent);
                $(".search-result").show();
            })
            .catch((error) => {
                console.error("Error fetching search results:", error);
            });
    }
}

// 4. Delete Single Contact Function
function deleteContact(cId) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: "Are you sure?",
            text: "You won't be able to revert this contact!",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Yes, delete it!"
        }).then((result) => {
            if (result.isConfirmed) {
                window.location = "/user/delete/" + cId;
            }
        });
    } else {
        if (confirm("Are you sure you want to delete this contact?")) {
            window.location = "/user/delete/" + cId;
        }
    }
}

// 5. Delete Entire Account Function (Danger Zone)
function deleteUserAccount() {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: "Delete Account?",
            text: "All your saved contacts and account data will be permanently deleted!",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#d33",
            cancelButtonColor: "#3085d6",
            confirmButtonText: "Yes, delete permanently!",
            cancelButtonText: "Cancel"
        }).then((result) => {
            if (result.isConfirmed) {
                window.location = "/user/delete-account";
            }
        });
    } else {
        if (confirm("Warning: All your saved contacts and data will be permanently deleted. Continue?")) {
            window.location = "/user/delete-account";
        }
    }
    // Instant AJAX Toggle Favorite Star
    function toggleFavorite(cId, starIcon) {
        fetch(`/user/toggle-favorite/${cId}`, {
            method: "POST"
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === "success") {
                if (data.isFavorite) {
                    starIcon.classList.remove("fa-regular", "text-muted");
                    starIcon.classList.add("fa-solid", "text-warning");
                } else {
                    starIcon.classList.remove("fa-solid", "text-warning");
                    starIcon.classList.add("fa-regular", "text-muted");
                }
            }
        })
        .catch(err => console.error("Error toggling favorite:", err));
    }
}